package com.mmc.merimusicplayer.data.sync.runtime

import com.mmc.merimusicplayer.data.model.sync.SyncData

interface SyncLocalApplyHost {
    suspend fun applyPlaylists(data: SyncData, expectedMutationVersion: Long): Boolean
    fun applyDeletions(data: SyncData, expectedMutationVersion: Long): Boolean
    suspend fun applyFavorites(data: SyncData, expectedMutationVersion: Long): Boolean
    suspend fun applyHistory(data: SyncData, remoteChanged: Boolean, expectedMutationVersion: Long): Boolean
    suspend fun applyStatistics(data: SyncData)
    suspend fun applyVideoSkipRules(data: SyncData, expectedMutationVersion: Long): Boolean
}
