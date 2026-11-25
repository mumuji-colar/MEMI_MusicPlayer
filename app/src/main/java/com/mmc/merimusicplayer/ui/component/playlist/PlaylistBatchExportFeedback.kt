package com.mmc.merimusicplayer.ui.component.playlist

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.data.local.playlist.LocalPlaylistRepository
import com.mmc.merimusicplayer.data.model.playlist.LocalPlaylistSongAddResult
import com.mmc.merimusicplayer.data.model.playlist.LocalPlaylist
import com.mmc.merimusicplayer.data.local.playlist.runLocalPlaylistMutationSafely
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.ui.feedback.showMemiSnackbar
import com.mmc.merimusicplayer.R as AppR

internal fun CoroutineScope.showPlaylistBatchExportCreatedResult(
    context: Context,
    snackbarHostState: SnackbarHostState,
    repository: LocalPlaylistRepository,
    result: Result<LocalPlaylist>
) {
    result.onSuccess { playlist ->
        showPlaylistBatchExportCreatedPlaylist(
            context = context,
            snackbarHostState = snackbarHostState,
            repository = repository,
            playlist = playlist
        )
    }.onFailure {
        showPlaylistBatchExportFailure(context, snackbarHostState)
    }
}

internal fun CoroutineScope.showPlaylistBatchExportAddedResult(
    context: Context,
    snackbarHostState: SnackbarHostState,
    repository: LocalPlaylistRepository,
    targetPlaylistId: Long,
    targetPlaylistName: String,
    result: Result<LocalPlaylistSongAddResult>
) {
    result.onSuccess { addResult ->
        showPlaylistBatchExportAddedSongs(
            context = context,
            snackbarHostState = snackbarHostState,
            repository = repository,
            targetPlaylistId = targetPlaylistId,
            targetPlaylistName = targetPlaylistName,
            addedSongs = addResult.addedSongs
        )
    }.onFailure {
        showPlaylistBatchExportFailure(context, snackbarHostState)
    }
}

internal fun CoroutineScope.showPlaylistBatchExportCreatedPlaylist(
    context: Context,
    snackbarHostState: SnackbarHostState,
    repository: LocalPlaylistRepository,
    playlist: LocalPlaylist
) {
    showPlaylistBatchExportSnackbar(
        context = context,
        snackbarHostState = snackbarHostState,
        targetPlaylistName = playlist.name,
        addedSongs = playlist.songs.toList(),
        undoOperation = {
            repository.deletePlaylist(playlist.id)
        }
    )
}

internal fun CoroutineScope.showPlaylistBatchExportAddedSongs(
    context: Context,
    snackbarHostState: SnackbarHostState,
    repository: LocalPlaylistRepository,
    targetPlaylistId: Long,
    targetPlaylistName: String,
    addedSongs: List<SongItem>
) {
    val undoSongs = addedSongs.toList()
    showPlaylistBatchExportSnackbar(
        context = context,
        snackbarHostState = snackbarHostState,
        targetPlaylistName = targetPlaylistName,
        addedSongs = undoSongs,
        undoOperation = {
            if (undoSongs.isNotEmpty()) {
                repository.removeSongsFromPlaylistByIdentity(targetPlaylistId, undoSongs)
            }
            true
        }
    )
}

internal fun CoroutineScope.showPlaylistBatchExportFailure(
    context: Context,
    snackbarHostState: SnackbarHostState
) {
    launch {
        snackbarHostState.showMemiSnackbar(context.getString(CoreCommonR.string.playlist_export_failed))
    }
}

private fun CoroutineScope.showPlaylistBatchExportSnackbar(
    context: Context,
    snackbarHostState: SnackbarHostState,
    targetPlaylistName: String,
    addedSongs: List<SongItem>,
    undoOperation: suspend () -> Boolean
) {
    val addedCount = addedSongs.size
    val message = context.resources.getQuantityString(
        AppR.plurals.playlist_batch_export_success,
        addedCount,
        addedCount,
        targetPlaylistName
    )
    launch {
        val result = snackbarHostState.showMemiSnackbar(
            message = message,
            actionLabel = context.getString(CoreCommonR.string.playlist_batch_export_undo),
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )
        if (result != SnackbarResult.ActionPerformed) return@launch

        val undoSucceeded = runLocalPlaylistMutationSafely("undoPlaylistBatchExport") {
            undoOperation()
        }.getOrDefault(false)
        val undoMessage = if (undoSucceeded) {
            context.getString(CoreCommonR.string.playlist_batch_export_undone, targetPlaylistName)
        } else {
            context.getString(CoreCommonR.string.playlist_batch_export_undo_failed)
        }
        snackbarHostState.showMemiSnackbar(undoMessage)
    }
}
