package com.mmc.merimusicplayer.data.sync.mapping

import android.content.Context
import com.mmc.merimusicplayer.data.model.playlist.DISPLAY_ORDER_SONG_ORDER_VERSION
import com.mmc.merimusicplayer.data.model.sync.SyncPlaylist
import com.mmc.merimusicplayer.data.model.playlist.LocalPlaylist

internal fun buildPlaylistSyncSnapshots(playlists: List<LocalPlaylist>, deletions: Map<Long, Long>, localizedContext: Context, optimizeLegacyLyrics: Boolean = false): List<SyncPlaylist> {
    val currentPlaylistIds = HashSet<Long>()
    val syncPlaylists = ArrayList<SyncPlaylist>(playlists.size)
    for (playlist in playlists) {
        currentPlaylistIds += playlist.id
        syncPlaylists += SyncPlaylist.fromLocalPlaylist(playlist, playlist.modifiedAt, localizedContext, optimizeLegacyLyrics)
    }

    deletions.forEach { (deletedId, deletedAt) ->
        if (deletedId !in currentPlaylistIds) {
            syncPlaylists += SyncPlaylist(
                id = deletedId,
                name = "",
                songs = emptyList(),
                createdAt = 0L,
                modifiedAt = deletedAt,
                isDeleted = true,
                songOrderVersion = DISPLAY_ORDER_SONG_ORDER_VERSION
            )
        }
    }

    return syncPlaylists
}
