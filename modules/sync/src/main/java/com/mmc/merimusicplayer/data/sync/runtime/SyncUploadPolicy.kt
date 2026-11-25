package com.mmc.merimusicplayer.data.sync.runtime

import com.mmc.merimusicplayer.data.model.sync.SyncData
import com.mmc.merimusicplayer.data.sync.change.SyncDataChangeDetector

object SyncUploadPolicy {
    fun shouldUpload(
        remoteData: SyncData?,
        requiresMigrationUpload: Boolean,
        mergedData: SyncData
    ): Boolean {
        if (remoteData == null || requiresMigrationUpload) {
            return true
        }
        return SyncDataChangeDetector.hasDataChanged(remoteData, mergedData)
    }
}
