package com.mmc.merimusicplayer.data.sync.host

import android.content.Context
import android.os.Build
import com.mmc.merimusicplayer.platform.bilibili.skip.BiliVideoSkipRepository
import com.mmc.merimusicplayer.data.history.PlayHistoryRepository
import com.mmc.merimusicplayer.data.local.media.LocalSongSupport
import com.mmc.merimusicplayer.data.local.playlist.LocalPlaylistRepository
import com.mmc.merimusicplayer.data.model.sync.SyncData
import com.mmc.merimusicplayer.data.model.sync.SyncFavoritePlaylist
import com.mmc.merimusicplayer.data.playlist.favorite.FavoritePlaylistRepository
import com.mmc.merimusicplayer.data.playlist.usage.LocalPlaylistPlaybackStatsRepository
import com.mmc.merimusicplayer.data.playlist.usage.PlaylistUsageRepository
import com.mmc.merimusicplayer.data.sync.store.github.SecureTokenStorage
import com.mmc.merimusicplayer.data.sync.mapping.fromFavoritePlaylist
import com.mmc.merimusicplayer.data.sync.mapping.buildPlaylistSyncSnapshots
import com.mmc.merimusicplayer.data.sync.mapping.buildRecentPlaySyncSnapshots
import com.mmc.merimusicplayer.data.sync.mapping.toSyncBiliVideoSkipRule
import com.mmc.merimusicplayer.data.sync.policy.SyncBiliVideoSkipMergePolicy
import com.mmc.merimusicplayer.data.sync.policy.copyWithNormalizedMembershipTokens
import com.mmc.merimusicplayer.data.sync.merge.song.SyncSongLyricMergePolicy

internal class AndroidSyncSnapshotBuilder(
    private val storage: SecureTokenStorage,
    private val playlistRepo: LocalPlaylistRepository,
    private val favoriteRepo: FavoritePlaylistRepository,
    private val playHistoryRepo: PlayHistoryRepository,
    private val playlistUsageRepo: PlaylistUsageRepository,
    private val localPlaylistPlaybackStatsRepo: LocalPlaylistPlaybackStatsRepository,
    private val biliVideoSkipRepo: BiliVideoSkipRepository
) {
    fun build(localizedContext: Context, playbackStatsClearedAt: Long): SyncData {
        val deviceId = getDeviceId()
        val syncPlaylists = buildPlaylistSyncSnapshots(playlistRepo.playlists.value, storage.getDeletedPlaylistTimestamps(), localizedContext)
        val syncFavoritePlaylists = favoritePlaylists(localizedContext)

        val syncRecentPlays = buildRecentPlaySyncSnapshots(playHistoryRepo.syncSnapshot(), { deviceId }, localizedContext)
        val syncRecentPlayDeletions = recentPlayDeletions()
        val syncPlaylistSongDeletions = playlistSongDeletions()
        val (syncPlaylistUsageStats, syncPlaylistUsageDeletions) = playlistUsageRepo.syncStatsAndDeletions()
        val localPlaylistPlaybackSnapshot = localPlaylistPlaybackStatsRepo.syncSnapshot()
        val syncBiliVideoSkipRules = videoSkipRules()

        val data = SyncData(
            deviceId = deviceId,
            deviceName = getDeviceName(),
            lastModified = System.currentTimeMillis(),
            playlists = syncPlaylists,
            favoritePlaylists = syncFavoritePlaylists,
            recentPlays = syncRecentPlays,
            syncLog = emptyList(),
            recentPlayDeletions = syncRecentPlayDeletions,
            playbackStatsClearedAt = playbackStatsClearedAt,
            playlistSongDeletions = syncPlaylistSongDeletions,
            playlistUsageStats = syncPlaylistUsageStats,
            playlistUsageDeletions = syncPlaylistUsageDeletions,
            localPlaylistPlaybackStats = localPlaylistPlaybackSnapshot.stats,
            localPlaylistPlaybackBuckets = localPlaylistPlaybackSnapshot.buckets,
            biliVideoSkipRules = syncBiliVideoSkipRules,
            lyricOverrides = storage.getLyricOverrides() + storage.getLegacyLyricCandidates()
        )
        return SyncSongLyricMergePolicy.prepareLegacy(data)
    }

    private fun recentPlayDeletions(): List<com.mmc.merimusicplayer.data.model.sync.SyncRecentPlayDeletion> {
        val syncRecentPlayDeletions = storage.getRecentPlayDeletions()
            .map {
                it.copy(mediaUri = LocalSongSupport.sanitizeMediaUriForSync(it.mediaUri))
            }
        return syncRecentPlayDeletions
    }

    private fun playlistSongDeletions(): List<com.mmc.merimusicplayer.data.model.sync.SyncPlaylistSongDeletion> {
        val syncPlaylistSongDeletions = storage.getPlaylistSongDeletions()
            .map {
                it.copyWithNormalizedMembershipTokens(
                    mediaUri = LocalSongSupport.sanitizeMediaUriForSync(it.mediaUri)
                )
            }

        return syncPlaylistSongDeletions
    }

    private fun favoritePlaylists(localizedContext: Context): List<SyncFavoritePlaylist> {
        val syncFavoritePlaylists = favoriteRepo.getSyncSnapshots().map {
            SyncFavoritePlaylist.fromFavoritePlaylist(it, localizedContext)
        }

        return syncFavoritePlaylists
    }

    private fun videoSkipRules(): List<com.mmc.merimusicplayer.data.model.sync.SyncBiliVideoSkipRule> {
        val syncBiliVideoSkipRules = SyncBiliVideoSkipMergePolicy.sanitize(
            biliVideoSkipRepo.snapshot().map { rule -> rule.toSyncBiliVideoSkipRule() }
        )

        return syncBiliVideoSkipRules
    }

    private fun getDeviceId(): String {
        return storage.getOrCreateDeviceId()
    }

    private fun getDeviceName(): String {
        return try {
            "${Build.MANUFACTURER} ${Build.MODEL}"
        } catch (_: Exception) {
            "Unknown Device"
        }
    }
}
