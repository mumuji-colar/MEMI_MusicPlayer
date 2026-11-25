package com.mmc.merimusicplayer.data.model.settings.bootstrap

import com.mmc.merimusicplayer.data.model.settings.download.DEFAULT_DOWNLOAD_PARALLELISM

data class BootstrapSettingsSnapshot(
    val bypassProxy: Boolean = true,
    val youtubeEnabled: Boolean = true,
    val preferHighRefreshRate: Boolean = true,
    val downloadDirectoryUri: String? = null,
    val downloadDirectoryLabel: String? = null,
    val downloadFileNameTemplate: String? = null,
    val downloadFollowPlaybackAudioQuality: Boolean = true,
    val downloadParallelism: Int = DEFAULT_DOWNLOAD_PARALLELISM
)
