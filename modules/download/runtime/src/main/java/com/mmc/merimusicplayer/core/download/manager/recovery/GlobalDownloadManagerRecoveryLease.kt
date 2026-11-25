package com.mmc.merimusicplayer.core.download.manager.recovery

import com.mmc.merimusicplayer.data.identity.stableKey

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import com.mmc.merimusicplayer.core.download.GlobalDownloadManager
import com.mmc.merimusicplayer.core.download.artifact.ManagedDownloadArtifactClaim
import com.mmc.merimusicplayer.data.model.download.DownloadExecutionRequest
import com.mmc.merimusicplayer.core.download.execution.persistence.DownloadExecutionRoomStore
import com.mmc.merimusicplayer.core.download.policy.commit.finalizedPublicationRecoveryLeaseOwnerId
import com.mmc.merimusicplayer.common.logging.NPLogger
import com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase
import com.mmc.merimusicplayer.data.local.database.entity.ManagedDownloadArtifactEntity
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.stableKey

internal data class ManagedDownloadArtifactRecoveryClaim(
    val claim: ManagedDownloadArtifactClaim,
    val request: DownloadExecutionRequest?,
    val leaseOwnerId: String
) {
    val artifact: ManagedDownloadArtifactEntity?
        get() = when (claim) {
            is ManagedDownloadArtifactClaim.Acquired -> claim.artifact
            is ManagedDownloadArtifactClaim.AlreadyDownloaded -> claim.artifact
            is ManagedDownloadArtifactClaim.RepairRequired -> claim.artifact
            is ManagedDownloadArtifactClaim.InFlight -> null
        }
}

private class RecoveryArtifactLeaseRebindRejected : IllegalStateException()

/** artifact 接管与 operation lease 写回必须在同一 Room 事务内完成 */
internal suspend fun GlobalDownloadManager.claimArtifactForRecovery(
    context: Context,
    song: SongItem,
    operationId: String?,
    expectedAttemptId: Long? = null,
    database: MemiUserDataDatabase = MemiUserDataDatabase.getInstance(context)
): ManagedDownloadArtifactRecoveryClaim? {
    val appContext = context.applicationContext
    val stableKey = song.stableKey()
    val normalizedOperationId = operationId?.trim()?.takeIf(String::isNotBlank)
    val recoveryLeaseOwnerId = finalizedPublicationRecoveryLeaseOwnerId(
        stableKey = stableKey,
        operationId = normalizedOperationId
    )
    return try {
        database.withTransaction {
            val request = normalizedOperationId
                ?.let { persistedOperationId ->
                    DownloadExecutionRoomStore.read(
                        context = appContext,
                        operationId = persistedOperationId,
                        database = database
                    )
                }
                ?.takeIf { persisted -> persisted.song.stableKey() == stableKey }
            if (expectedAttemptId != null && request?.attemptId != expectedAttemptId) {
                return@withTransaction null
            }
            val claim = managedDownloadArtifactCoordinator.claim(
                context = appContext,
                song = song,
                reconcileStorage = false,
                leaseOwnerId = recoveryLeaseOwnerId,
                allowFreshTransferReclaim = false,
                allowPostCoreRecoveryReclaim = true,
                postCoreRecoveryPreviousLeaseId = request?.artifactLeaseId,
                databaseOverride = database
            )
            if (claim is ManagedDownloadArtifactClaim.InFlight) {
                return@withTransaction null
            }
            val claimedLeaseId = when (claim) {
                is ManagedDownloadArtifactClaim.Acquired -> claim.artifact.leaseId
                is ManagedDownloadArtifactClaim.AlreadyDownloaded -> claim.artifact.leaseId
                is ManagedDownloadArtifactClaim.RepairRequired -> claim.artifact.leaseId
                is ManagedDownloadArtifactClaim.InFlight -> null
            }
            if (claimedLeaseId != null && claimedLeaseId != recoveryLeaseOwnerId) {
                return@withTransaction null
            }
            if (request != null && claimedLeaseId != recoveryLeaseOwnerId) {
                throw RecoveryArtifactLeaseRebindRejected()
            }
            if (
                request != null &&
                    request.artifactLeaseId != recoveryLeaseOwnerId &&
                    !DownloadExecutionRoomStore.rebindArtifactLeaseForRecovery(
                        context = appContext,
                        operationId = request.operationId,
                        stableKey = stableKey,
                        expectedArtifactLeaseId = request.artifactLeaseId,
                        recoveryArtifactLeaseId = recoveryLeaseOwnerId,
                        database = database
                    )
            ) {
                throw RecoveryArtifactLeaseRebindRejected()
            }
            ManagedDownloadArtifactRecoveryClaim(
                claim = claim,
                request = request,
                leaseOwnerId = recoveryLeaseOwnerId
            )
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: RecoveryArtifactLeaseRebindRejected) {
        NPLogger.w(
            TAG,
            "恢复 artifact lease 未能与 operation 原子写回，保留等待重试: " +
                "song=${song.name}, operationId=$normalizedOperationId"
        )
        null
    } catch (error: Throwable) {
        NPLogger.w(
            TAG,
            "恢复 artifact lease 失败，保留等待重试: " +
                "song=${song.name}, operationId=$normalizedOperationId, " +
                "error=${error.message}",
            error
        )
        null
    }
}
