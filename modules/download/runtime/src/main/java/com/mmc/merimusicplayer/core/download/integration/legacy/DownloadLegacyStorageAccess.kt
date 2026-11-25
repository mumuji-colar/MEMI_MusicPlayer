package com.mmc.merimusicplayer.core.download.integration.legacy

import android.content.Context
import com.mmc.merimusicplayer.core.download.catalog.DownloadedSongCatalogRoomStore
import com.mmc.merimusicplayer.core.download.execution.persistence.DownloadExecutionRoomStore
import com.mmc.merimusicplayer.core.download.storage.ManagedDownloadStorageJsonCodec
import com.mmc.merimusicplayer.core.download.storage.queue.DownloadRecoveryRoomStore
import com.mmc.merimusicplayer.core.download.storage.snapshot.ManagedDownloadSnapshotRoomStore
import com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase
import com.mmc.merimusicplayer.data.model.download.DownloadExecutionRequest
import com.mmc.merimusicplayer.data.model.download.DownloadedAudioMetadata
import org.json.JSONObject

object DownloadLegacyStorageAccess {
    const val PENDING_QUEUE_CUTOVER_STATE_KEY = DownloadRecoveryRoomStore.PENDING_QUEUE_CUTOVER_STATE_KEY
    const val CANCELLED_KEYS_CUTOVER_STATE_KEY = DownloadRecoveryRoomStore.CANCELLED_KEYS_CUTOVER_STATE_KEY
    const val USER_CLEARED_STATE = DownloadRecoveryRoomStore.USER_CLEARED_STATE
    const val CATALOG_CUTOVER_STATE_KEY = DownloadedSongCatalogRoomStore.CUTOVER_STATE_METADATA_KEY
    const val SNAPSHOT_CUTOVER_STATE_KEY = ManagedDownloadSnapshotRoomStore.CUTOVER_STATE_METADATA_KEY

    fun serializeAudioMetadata(metadata: DownloadedAudioMetadata): JSONObject =
        ManagedDownloadStorageJsonCodec.downloadedAudioMetadataToJson(metadata)

    suspend fun bootstrapLegacyQueues(context: Context) {
        DownloadRecoveryRoomStore(context).bootstrapLegacyFilesOnce()
    }

    suspend fun upsertOperation(
        context: Context,
        request: DownloadExecutionRequest,
        state: String,
        queueOrder: Int,
        database: MemiUserDataDatabase
    ) {
        DownloadExecutionRoomStore.upsert(context, request, state, queueOrder, database = database)
    }

    suspend fun updateOperationState(
        context: Context,
        operationId: String,
        state: String,
        errorCode: String?,
        database: MemiUserDataDatabase
    ): Boolean = DownloadExecutionRoomStore.updateState(
        context, operationId, state, errorCode, database
    )
}
