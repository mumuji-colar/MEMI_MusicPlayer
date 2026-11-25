package com.mmc.merimusicplayer.data.model.settings.download

data class DownloadAudioQualitySelection(
    val neteaseQuality: String,
    val youtubeQuality: String,
    val biliQuality: String
) {
    companion object
}
