package com.mmc.merimusicplayer.ui.screen.server

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.ui.screen.playlist.detail.PlaylistModernSongRow
import com.mmc.merimusicplayer.ui.screen.server.album.ServerAlbumSongMenu
import com.mmc.merimusicplayer.ui.util.rememberSongDisplayCoverUrl
import com.mmc.merimusicplayer.ui.theme.MemiDesign

@Composable
internal fun MusicServerSongRow(
    song: SongItem,
    index: Int,
    current: Boolean,
    playing: Boolean,
    favorite: Boolean,
    favoriteBusy: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    downloaded: Boolean,
    onClick: () -> Unit,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    offlineMode: Boolean = false,
    downloadPresenceVersion: Int = 0
) {
    val cover = rememberSongDisplayCoverUrl(song, resolveLocalFallback = false)
    PlaylistModernSongRow(
        song = song, index = index - 1, coverUrl = cover, offlineMode = offlineMode,
        selectionMode = selectionMode, selected = selected, current = current, playing = playing,
        downloaded = downloaded, downloadPresenceVersion = downloadPresenceVersion,
        onClick = { if (selectionMode) onSelect() else onClick() },
        onLongClick = onSelect, onToggleSelection = onSelect,
        modifier = Modifier.clip(MemiDesign.tokens.cardShape).semantics { this.selected = selected || current },
        compact = true
    ) {
        if (!selectionMode) {
            ServerAlbumSongMenu(
                song = song, favorite = favorite, favoriteBusy = favoriteBusy, downloaded = downloaded,
                onFavorite = onFavorite, onDownload = onDownload, onAddToPlaylist = onAddToPlaylist,
                onPlayNext = onPlayNext, onAddToQueue = onAddToQueue
            )
        }
    }
}
