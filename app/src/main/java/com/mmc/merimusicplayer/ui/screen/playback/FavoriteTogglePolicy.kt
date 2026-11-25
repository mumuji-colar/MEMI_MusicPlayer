package com.mmc.merimusicplayer.ui.screen.playback

import android.content.Context
import com.mmc.merimusicplayer.data.local.playlist.system.FavoritesPlaylist
import com.mmc.merimusicplayer.data.model.SongItem

internal fun nextFavoriteStateAfterTap(displayedIsFavorite: Boolean): Boolean {
    return !displayedIsFavorite
}

internal fun shouldConfirmFavoriteSync(song: SongItem?, adding: Boolean, context: Context? = null): Boolean =
    adding && song != null && FavoritesPlaylist.toSyncableSongOrNull(song, context) == null

internal fun shouldConfirmFavoriteSync(songs: List<SongItem>, context: Context? = null): Boolean =
    songs.any { shouldConfirmFavoriteSync(it, adding = true, context = context) }
