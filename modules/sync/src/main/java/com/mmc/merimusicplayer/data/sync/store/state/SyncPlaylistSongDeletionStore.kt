package com.mmc.merimusicplayer.data.sync.store.state

import android.content.SharedPreferences
import com.google.gson.reflect.TypeToken
import com.mmc.merimusicplayer.data.model.SongIdentity
import com.mmc.merimusicplayer.data.model.sync.SyncPlaylistSongDeletion
import com.mmc.merimusicplayer.data.sync.merge.playlist.SyncPlaylistDeletionPolicy

internal class SyncPlaylistSongDeletionStore(encryptedPrefs: SharedPreferences, private val files: SyncDeletionStateStorage) {
    private val mutation = SyncMutationVersionStore(encryptedPrefs)

    fun getPlaylistSongDeletions(): List<SyncPlaylistSongDeletion> {
        return synchronized(syncMutationLock) {
            val type = object : TypeToken<List<SyncPlaylistSongDeletion>>() {}.type
            val parsed = files.read<List<SyncPlaylistSongDeletion>>(KEY_PLAYLIST_SONG_DELETIONS, type).orEmpty()
            normalizePlaylistSongDeletions(parsed)
        }
    }

    fun setPlaylistSongDeletions(deletions: List<SyncPlaylistSongDeletion>) {
        synchronized(syncMutationLock) {
            persistPlaylistSongDeletionsLocked(
                deletions = normalizePlaylistSongDeletions(deletions),
                bumpVersion = false
            )
        }
    }

    fun addPlaylistSongDeletions(deletions: List<SyncPlaylistSongDeletion>) {
        if (deletions.isEmpty()) {
            return
        }
        synchronized(syncMutationLock) {
            persistPlaylistSongDeletionsLocked(
                deletions = normalizePlaylistSongDeletions(getPlaylistSongDeletions() + deletions),
                bumpVersion = true
            )
        }
    }

    fun removePlaylistSongDeletions(
        playlistId: Long,
        identities: Collection<SongIdentity>
    ) {
        if (identities.isEmpty()) {
            return
        }
        synchronized(syncMutationLock) {
            val current = getPlaylistSongDeletions()
            val remaining = SyncPlaylistDeletionPolicy.clearLegacyDeletionsForReaddedSongs(
                deletions = current,
                playlistId = playlistId,
                identities = identities
            )
            persistPlaylistSongDeletionsLocked(remaining, bumpVersion = true, changed = remaining != current)
        }
    }

    fun removePlaylistSongDeletionsForPlaylist(playlistId: Long) {
        synchronized(syncMutationLock) {
            val current = getPlaylistSongDeletions()
            val remaining = current
                .filterNot { it.playlistId == playlistId }
            persistPlaylistSongDeletionsLocked(remaining, bumpVersion = true, changed = remaining != current)
        }
    }

    private fun persistPlaylistSongDeletionsLocked(
        deletions: List<SyncPlaylistSongDeletion>,
        bumpVersion: Boolean,
        changed: Boolean = true
    ) {
        check(
            files.commitEdit {
                if (changed) {
                    val normalized = normalizePlaylistSongDeletions(deletions)
                    files.write(this, KEY_PLAYLIST_SONG_DELETIONS, normalized.takeIf { it.isNotEmpty() })
                    if (bumpVersion) mutation.bump(this)
                }
            }
        ) { "Failed to persist playlist deletion state" }
    }

    private fun normalizePlaylistSongDeletions(
        deletions: List<SyncPlaylistSongDeletion>
    ): List<SyncPlaylistSongDeletion> {
        return SyncPlaylistDeletionPolicy.mergeDeletions(deletions, emptyList())
    }
}
