package com.mmc.merimusicplayer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mmc.merimusicplayer.data.model.download.DownloadTask

@Composable
fun ActiveDownloadTaskList(
    tasks: List<DownloadTask>,
    modifier: Modifier = Modifier,
    maxVisibleTasks: Int = Int.MAX_VALUE,
    maxHeight: Dp = 320.dp
) {
    com.mmc.merimusicplayer.ui.component.download.ActiveDownloadTaskList(
        tasks = tasks,
        modifier = modifier,
        maxVisibleTasks = maxVisibleTasks,
        maxHeight = maxHeight
    )
}

@Composable
fun BatchDownloadManagerSheet(
    downloadTasks: List<DownloadTask>,
    onDismiss: () -> Unit
) {
    com.mmc.merimusicplayer.ui.component.download.BatchDownloadManagerSheet(
        downloadTasks = downloadTasks,
        onDismiss = onDismiss
    )
}

@Composable
internal fun SongDownloadSubtitle(
    text: String,
    downloaded: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    com.mmc.merimusicplayer.ui.component.download.SongDownloadSubtitle(
        text = text,
        downloaded = downloaded,
        modifier = modifier,
        contentDescription = contentDescription
    )
}
