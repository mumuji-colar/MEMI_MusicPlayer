package com.mmc.merimusicplayer.data.sync.store.state

import android.content.SharedPreferences
import com.mmc.merimusicplayer.data.model.sync.SyncPlaylistSongDeletion
import com.mmc.merimusicplayer.data.model.sync.SyncRecentPlayDeletion
import com.mmc.merimusicplayer.data.sync.merge.playlist.SyncPlaylistDeletionPolicy
import com.mmc.merimusicplayer.data.sync.merge.history.SyncRecentPlayMerger

internal class SyncDeletionStateCommitter(private val encryptedPrefs: SharedPreferences, private val files: SyncDeletionStateStorage) {
    private val recent = SyncRecentPlayDeletionStore(encryptedPrefs, files)
    private val songs = SyncPlaylistSongDeletionStore(encryptedPrefs, files)

    fun setDeletionStateIfMutationVersion(
        expectedMutationVersion: Long,
        recentPlayDeletions: List<SyncRecentPlayDeletion>,
        playlistSongDeletions: List<SyncPlaylistSongDeletion>
    ): Boolean {
        val normalizedRecent = SyncRecentPlayMerger.mergeRecentPlayDeletions(recentPlayDeletions, emptyList())
        val normalizedPlaylist = normalizePlaylistSongDeletions(playlistSongDeletions)
        return synchronized(syncMutationLock) {
            if (encryptedPrefs.getLong(KEY_SYNC_MUTATION_VERSION, 0L) != expectedMutationVersion) {
                return@synchronized false
            }
            val recentChanged = normalizedRecent != recent.getRecentPlayDeletions()
            val playlistChanged = normalizedPlaylist != songs.getPlaylistSongDeletions()
            // 相等时仍确认偏好耐久性，之前失败的 commit 可能只更新了内存
            check(
                files.commitEdit {
                    if (recentChanged) {
                        files.write(this, KEY_RECENT_PLAY_DELETIONS, normalizedRecent.takeIf { it.isNotEmpty() })
                    }
                    if (playlistChanged) {
                        files.write(this, KEY_PLAYLIST_SONG_DELETIONS, normalizedPlaylist.takeIf { it.isNotEmpty() })
                    }
                }
            ) { "Failed to persist guarded sync deletion state" }
            true
        }
    }

    private fun normalizePlaylistSongDeletions(
        deletions: List<SyncPlaylistSongDeletion>
    ): List<SyncPlaylistSongDeletion> {
        return SyncPlaylistDeletionPolicy.mergeDeletions(deletions, emptyList())
    }
}
