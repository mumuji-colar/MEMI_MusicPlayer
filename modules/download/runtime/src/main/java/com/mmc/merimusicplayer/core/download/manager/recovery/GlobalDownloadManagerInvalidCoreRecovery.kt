package com.mmc.merimusicplayer.core.download.manager.recovery

import com.mmc.merimusicplayer.data.identity.stableKey

import android.content.Context
import androidx.room.withTransaction
import com.mmc.merimusicplayer.core.download.GlobalDownloadManager
import com.mmc.merimusicplayer.core.download.ManagedDownloadStorage
import com.mmc.merimusicplayer.core.download.execution.clear.DownloadStorageMutationDeferredException
import com.mmc.merimusicplayer.core.download.execution.clear.ManagedDownloadDirectoryMutationFence
import com.mmc.merimusicplayer.core.download.execution.persistence.DownloadExecutionRoomStore
import com.mmc.merimusicplayer.data.model.download.DownloadExecutionRequest
import com.mmc.merimusicplayer.core.download.execution.retry.DOWNLOAD_INTEGRITY_MAX_FAILURES
import com.mmc.merimusicplayer.core.download.manager.commit.inspectFinalizedDownloadedAudio
import com.mmc.merimusicplayer.core.download.manager.runtime.isRecoveryMetadataOwnedBySong
import com.mmc.merimusicplayer.core.download.manager.runtime.loadFinalizationRecoverySnapshot
import com.mmc.merimusicplayer.core.download.manager.batch.forgetPendingDownloadQueueEntriesForOperation
import com.mmc.merimusicplayer.core.download.manager.runtime.wakeDownloadExecutionPump
import com.mmc.merimusicplayer.core.download.storage.reference.ManagedDownloadReferenceLookup
import com.mmc.merimusicplayer.data.model.download.DownloadStatus
import com.mmc.merimusicplayer.core.download.policy.hasDownloadedAudioDurationMismatch
import com.mmc.merimusicplayer.core.download.policy.expectedDownloadedAudioDurationMs
import com.mmc.merimusicplayer.common.logging.NPLogger
import com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase
import com.mmc.merimusicplayer.data.local.database.entity.DownloadBatchMemberTerminal
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.stableKey

internal suspend fun GlobalDownloadManager.invalidCoreAudioReason(
    context: Context,
    song: SongItem,
    audio: ManagedDownloadStorage.StoredEntry,
    operationId: String
): String? {
    val metadata = readDownloadedMetadata(context, audio)
    if (metadata != null && !isRecoveryMetadataOwnedBySong(metadata, song, operationId)) {
        return "CORE_AUDIO_IDENTITY_MISMATCH"
    }
    val probe = inspectFinalizedDownloadedAudio(context, audio)
    return "CORE_AUDIO_DURATION_MISMATCH".takeIf {
        probe.readable && hasDownloadedAudioDurationMismatch(
            expectedDownloadedAudioDurationMs(song, metadata), probe.durationMs
        )
    }
}

internal suspend fun GlobalDownloadManager.requeueInvalidCoreAudio(
    context: Context,
    song: SongItem,
    operationId: String,
    audioReference: String?,
    expectedLeaseId: String?,
    reason: String,
    directoryLeaseOwned: Boolean = false
): Boolean {
    val directoryLease = if (directoryLeaseOwned) null else {
        ManagedDownloadDirectoryMutationFence.acquireCommitLeaseOrNull(context, operationId)
            ?: throw DownloadStorageMutationDeferredException(operationId)
    }
    try {
        val result = resetInvalidCoreDownloadForTransfer(
            context, song, operationId, audioReference, expectedLeaseId, reason
        ) ?: return false
        val request = result.request
        if (result.retryExhausted) {
            request.attemptId?.let { attemptId ->
                updateTaskStatus(song.stableKey(), DownloadStatus.FAILED, expectedAttemptId = attemptId)
            }
            forgetPendingDownloadQueueEntriesForOperation(context, song.stableKey(), operationId)
            NPLogger.w(TAG, "core 音频恢复次数已耗尽，保留文件供检查和手动重试: operationId=$operationId, reason=$reason")
            return true
        }
        NPLogger.w(TAG, "core 音频凭据无效，保留原文件并按原队号重新传输: operationId=$operationId, reason=$reason")
        request.attemptId?.let { attemptId ->
            updateTaskStatus(song.stableKey(), DownloadStatus.QUEUED, expectedAttemptId = attemptId)
        }
        wakeDownloadExecutionPump(context, reason = reason)
        return true
    } finally {
        directoryLease?.close()
    }
}

internal data class InvalidCoreTransferReset(
    val request: DownloadExecutionRequest,
    val retryExhausted: Boolean
)

internal suspend fun resetInvalidCoreDownloadForTransfer(
    context: Context,
    song: SongItem,
    operationId: String,
    audioReference: String?,
    expectedLeaseId: String?,
    reason: String,
    database: MemiUserDataDatabase = MemiUserDataDatabase.getInstance(context)
): InvalidCoreTransferReset? {
    return database.withTransaction {
        val dao = database.downloadOperationDao()
        val header = dao.findHeader(operationId) ?: return@withTransaction null
        if (header.stableKey != song.stableKey() || header.stopRequestedByUser) {
            return@withTransaction null
        }
        val request = DownloadExecutionRoomStore.read(context, operationId, database)
            ?: return@withTransaction null
        val artifactDao = database.managedDownloadArtifactDao()
        val artifact = artifactDao.findAllByStableKey(song.stableKey()).singleOrNull {
            it.audioReference == audioReference && it.leaseId == expectedLeaseId
        } ?: return@withTransaction null
        if (dao.resetInvalidCoreForTransfer(
                operationId, song.stableKey(), header.updatedAtMs, reason, System.currentTimeMillis()
            ) != 1
        ) return@withTransaction null
        val retryExhausted = header.retryCount >= DOWNLOAD_INTEGRITY_MAX_FAILURES - 1
        // 只解绑错误引用，保留原文件和 sidecar，不能删除或继续改写另一首歌
        artifactDao.upsert(artifact.copy(
            state = if (retryExhausted) "FAILED_RETRYABLE" else "QUEUED",
            leaseId = request.artifactLeaseId.takeUnless { retryExhausted },
            audioReference = null, audioName = null, fileSize = null, contentHash = null,
            finalizedAtMs = null, needsReconcile = true, lastErrorCode = reason,
            updatedAtMs = System.currentTimeMillis()
        ))
        val transferRequest = request.copy(preserveStaging = false, requiresFreshTransfer = true)
        DownloadExecutionRoomStore.upsert(
            context, transferRequest, state = "RETRYABLE", database = database
        )
        if (retryExhausted) {
            // 音频重传与收尾共享预算，失败终态和批次计数必须一起提交
            check(DownloadExecutionRoomStore.updateState(
                context, operationId, "INVALID", "${reason}_RETRY_EXHAUSTED",
                database = database, expectedAttemptId = request.attemptId
            ))
            DownloadExecutionRoomStore.markBatchMembersForOperation(
                context, operationId, header.stableKey, request.attemptId,
                DownloadBatchMemberTerminal.FAILED, database = database
            )
            dao.deleteHostAdmission(operationId)
        }
        InvalidCoreTransferReset(transferRequest, retryExhausted)
    }
}

internal suspend fun GlobalDownloadManager.requeueConfirmedMissingCoreAudio(
    context: Context,
    song: SongItem,
    operationId: String,
    audioReference: String?,
    audioName: String?,
    expectedLeaseId: String?
): Boolean {
    val directoryLease = ManagedDownloadDirectoryMutationFence.acquireCommitLeaseOrNull(context, operationId)
        ?: throw DownloadStorageMutationDeferredException(operationId)
    try {
        if (downloadedSongDeletionCounts.containsKey(song.stableKey())) return false
        val evidence = ManagedDownloadReferenceLookup.inspect(context, audioReference)
        if (!audioReference.isNullOrBlank() && evidence != ManagedDownloadReferenceLookup.Result.Missing) {
            return false
        }
        val snapshot = loadFinalizationRecoverySnapshot(
            context, forceRefresh = true, allowFreshCacheReuse = false
        ) ?: return false
        if (!snapshot.rootEntriesComplete) return false
        // 引用丢失也可能是 pending 正在提升为正式文件，完整扫描确认前不能重传
        val candidates = snapshot.audioEntries + snapshot.pendingAudioEntries +
            snapshot.audioEntriesWithoutMetadata
        if (candidates.any { audio ->
                val metadata = ManagedDownloadStorage.metadataForAudioEntry(snapshot, audio)
                (metadata != null && isRecoveryMetadataOwnedBySong(metadata, song, operationId)) ||
                    (!audioName.isNullOrBlank() &&
                        (audio.name == audioName || audio.logicalName == audioName))
            }
        ) return false
        return requeueInvalidCoreAudio(
            context, song, operationId, audioReference, expectedLeaseId, "CORE_AUDIO_MISSING_CONFIRMED",
            directoryLeaseOwned = true
        )
    } finally {
        directoryLease.close()
    }
}
