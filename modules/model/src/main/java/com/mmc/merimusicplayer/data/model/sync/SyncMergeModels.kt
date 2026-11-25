package com.mmc.merimusicplayer.data.model.sync

data class SyncMergeResult(val mergedData: SyncData, val syncResult: SyncResult)

data class LocalPlaylistPlaybackSyncResult(
    val stats: List<SyncLocalPlaylistPlaybackStat>,
    val buckets: List<SyncLocalPlaylistPlaybackBucket>
)

data class SyncSystemPlaylist(val id: Long, val currentName: String)
