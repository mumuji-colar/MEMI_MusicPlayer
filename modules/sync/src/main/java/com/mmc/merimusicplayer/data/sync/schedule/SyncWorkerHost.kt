package com.mmc.merimusicplayer.data.sync.schedule

import com.mmc.merimusicplayer.data.model.sync.SyncResult
import com.mmc.merimusicplayer.data.model.sync.SyncWorkerOutcome

interface SyncWorkerHost {
    fun autoSyncEnabled(): Boolean
    fun configured(): Boolean
    suspend fun protocolUpgradeApproved(): Boolean
    fun playbackActive(): Boolean
    fun validatedNetwork(): Boolean
    fun deferForPlayback()
    suspend fun synchronize(): Result<SyncResult>
    suspend fun handleFailure(error: Throwable?, manual: Boolean, unexpected: Boolean): SyncWorkerOutcome
}
