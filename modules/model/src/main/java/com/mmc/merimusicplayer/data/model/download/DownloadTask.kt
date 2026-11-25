package com.mmc.merimusicplayer.data.model.download

import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.download.DownloadStatus

data class DownloadTask(
    val song: SongItem,
    val progress: DownloadProgress?,
    val status: DownloadStatus,
    val attemptId: Long = 0L,
    val failureReason: DownloadFailureReason? = null
)
