package com.mmc.merimusicplayer.core.download.storage.queue

import android.content.Context
import com.mmc.merimusicplayer.core.download.ManagedDownloadStorage
import com.mmc.merimusicplayer.data.local.database.entity.DownloadOperationEntity
import com.mmc.merimusicplayer.data.model.SongItem

abstract class DownloadRecoveryRoomStoreTestSupport {

















































    internal fun largeCancelledOperation(
        operationId: String,
        stableKey: String,
        state: String = "CANCEL_REQUESTED",
        libraryId: String
    ): DownloadOperationEntity {
        return DownloadOperationEntity(
            operationId = operationId,
            stableKey = stableKey,
            libraryId = libraryId,
            state = state,
            queueOrder = 0,
            sourceHintJson = "{}",
            stagingDirName = operationId,
            bytesWritten = 0L,
            totalBytes = null,
            resumeJson = null,
            retryCount = 0,
            nextRetryAtMs = null,
            lastErrorCode = null,
            createdAtMs = 1L,
            updatedAtMs = 1L
        )
    }

    internal fun currentLibraryId(context: Context): String {
        return ManagedDownloadStorage.currentSnapshotCacheKey(context)
    }

    internal fun song(id: Long, name: String): SongItem {
        return SongItem(
            id = id,
            name = name,
            artist = "artist",
            album = "album",
            albumId = 10L,
            durationMs = 180_000L,
            coverUrl = null,
            channelId = "netease",
            audioId = id.toString()
        )
    }

    internal companion object {
        const val LARGE_OPERATION_COUNT = 1_001
    }

}
