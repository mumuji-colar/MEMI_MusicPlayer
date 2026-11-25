package com.mmc.merimusicplayer.ui.component

import androidx.compose.runtime.Composable
import com.mmc.merimusicplayer.data.model.SongItem

@Composable
fun LocalSongDetailsDialog(
    song: SongItem,
    onDismiss: () -> Unit
) {
    com.mmc.merimusicplayer.ui.component.local.LocalSongDetailsDialog(
        song = song,
        onDismiss = onDismiss
    )
}

@Composable
fun LocalSongSyncConfirmDialog(
    actionLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    com.mmc.merimusicplayer.ui.component.local.LocalSongSyncConfirmDialog(
        actionLabel = actionLabel,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
