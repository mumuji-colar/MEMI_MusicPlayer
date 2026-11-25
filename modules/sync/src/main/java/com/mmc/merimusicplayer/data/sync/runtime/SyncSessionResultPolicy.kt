package com.mmc.merimusicplayer.data.sync.runtime

import com.mmc.merimusicplayer.data.sync.runtime.dataset.SyncDatasetMergeResult
import com.mmc.merimusicplayer.data.model.sync.SyncResult
import com.mmc.merimusicplayer.data.model.sync.SyncResultMessage
import com.mmc.merimusicplayer.data.model.sync.SyncUploadResolution

internal object SyncSessionResultPolicy {
    fun <TVersion> result(
        resolution: SyncUploadResolution<SyncDatasetMergeResult, TVersion>,
        firstSync: Boolean,
        initialRemoteMissing: Boolean,
        noChangeMessage: String,
        initialUploadMessage: String
    ): SyncResult = when {
        !resolution.uploadPerformed && !resolution.remoteChangedDuringSync && !firstSync ->
            SyncResult(success = true, message = noChangeMessage, messageKind = SyncResultMessage.NO_CHANGE)
        initialRemoteMissing && resolution.uploadPerformed && !resolution.remoteChangedDuringSync ->
            SyncResult(success = true, message = initialUploadMessage, messageKind = SyncResultMessage.INITIAL_UPLOADED)
        else -> resolution.merged.syncResult
    }
}
