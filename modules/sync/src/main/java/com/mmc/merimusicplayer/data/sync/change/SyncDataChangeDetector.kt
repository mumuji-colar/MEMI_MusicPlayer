package com.mmc.merimusicplayer.data.sync.change

import com.mmc.merimusicplayer.data.model.sync.SyncData
import com.mmc.merimusicplayer.data.sync.policy.SyncBiliVideoSkipMergePolicy

object SyncDataChangeDetector {
    fun hasDataChanged(remote: SyncData, merged: SyncData): Boolean =
        SyncPlaylistChangeDetector.playlistsChanged(remote, merged) ||
            remote.lyricOverrides != merged.lyricOverrides ||
            SyncPlaylistChangeDetector.favoritesChanged(remote, merged) ||
            SyncHistoryChangeDetector.changed(remote, merged) ||
            !SyncBiliVideoSkipMergePolicy.same(remote.biliVideoSkipRules, merged.biliVideoSkipRules) ||
            SyncStatisticsChangeDetector.playbackChanged(remote, merged) ||
            SyncStatisticsChangeDetector.playlistUsageChanged(remote, merged)
}
