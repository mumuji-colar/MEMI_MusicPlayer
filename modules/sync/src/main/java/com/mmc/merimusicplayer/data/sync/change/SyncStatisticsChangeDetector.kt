package com.mmc.merimusicplayer.data.sync.change

import com.mmc.merimusicplayer.data.model.sync.SyncData
import com.mmc.merimusicplayer.data.model.sync.SyncPlaylistUsageDeletionPolicy
import com.mmc.merimusicplayer.data.sync.mapping.stats.SyncPlaybackStatMapping
import com.mmc.merimusicplayer.data.sync.merge.stats.SyncPlaylistUsageStatsMergePolicy

internal object SyncStatisticsChangeDetector {
    fun playbackChanged(remote: SyncData, merged: SyncData): Boolean =
        remote.playbackStatsClearedAt != merged.playbackStatsClearedAt ||
            SyncCollectionComparison.keyedChanged(remote.playbackStats, merged.playbackStats, { it.identityKey }, SyncPlaybackStatMapping::sameMetadata) ||
            SyncCollectionComparison.keyedChanged(remote.playbackStatBuckets, merged.playbackStatBuckets, { it.dayStartAt to it.identityKey }, SyncPlaybackStatMapping::sameMetadata)

    fun playlistUsageChanged(remote: SyncData, merged: SyncData): Boolean =
        SyncPlaylistUsageDeletionPolicy.merge(remote.playlistUsageDeletions) != SyncPlaylistUsageDeletionPolicy.merge(merged.playlistUsageDeletions) ||
        SyncCollectionComparison.keyedChanged(remote.playlistUsageStats, merged.playlistUsageStats, { it.playlistKey }, SyncPlaylistUsageStatsMergePolicy::same) ||
            SyncCollectionComparison.keyedChanged(remote.localPlaylistPlaybackStats, merged.localPlaylistPlaybackStats, { it.playlistId }, SyncPlaylistUsageStatsMergePolicy::same) ||
            SyncCollectionComparison.keyedChanged(remote.localPlaylistPlaybackBuckets, merged.localPlaylistPlaybackBuckets, { it.playlistId to it.dayStartAt }, SyncPlaylistUsageStatsMergePolicy::same)
}
