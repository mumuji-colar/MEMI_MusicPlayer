package com.mmc.merimusicplayer.ui.screen.server.album

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.mmc.merimusicplayer.data.local.playlist.LocalPlaylistRepository
import com.mmc.merimusicplayer.data.local.playlist.launchLocalPlaylistMutation
import com.mmc.merimusicplayer.data.local.playlist.system.LocalFilesPlaylist
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.playlist.LocalPlaylist
import com.mmc.merimusicplayer.ui.component.playlist.PlaylistExportSheet
import com.mmc.merimusicplayer.ui.component.playlist.showPlaylistBatchExportAddedResult
import com.mmc.merimusicplayer.ui.component.playlist.showPlaylistBatchExportCreatedResult
import com.mmc.merimusicplayer.common.R as CoreCommonR

@Composable
internal fun ServerAlbumExportSheet(
    songs: List<SongItem>,
    playlists: List<LocalPlaylist>,
    repository: LocalPlaylistRepository,
    snackbar: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    PlaylistExportSheet(
        title = stringResource(CoreCommonR.string.playlist_add_to),
        playlists = playlists.filterNot { LocalFilesPlaylist.isSystemPlaylist(it, context) },
        selectedCount = songs.size, onDismissRequest = onDismiss,
        createActionLabel = stringResource(CoreCommonR.string.playlist_create_and_add),
        confirmationTitle = stringResource(CoreCommonR.string.server_add_playlist_confirm_title),
        confirmationMessage = { name -> resources.getQuantityString(
            CoreCommonR.plurals.server_add_playlist_confirm_message, songs.size, songs.size, name
        ) },
        confirmActionLabel = stringResource(CoreCommonR.string.server_add_playlist_confirm_action),
        onCreateAndExport = { name ->
            scope.launchLocalPlaylistMutation(
                operation = "createPlaylistFromServerAlbum",
                onResult = { scope.showPlaylistBatchExportCreatedResult(context, snackbar, repository, it) }
            ) { repository.createPlaylistWithSongs(name, songs) }
        },
        onExportToPlaylist = { playlist ->
            scope.launchLocalPlaylistMutation(
                operation = "addServerAlbumSongsToPlaylist",
                onResult = { scope.showPlaylistBatchExportAddedResult(context, snackbar, repository, playlist.id, playlist.name, it) }
            ) { repository.addSongsToPlaylistWithResult(playlist.id, songs) }
        }
    )
}
