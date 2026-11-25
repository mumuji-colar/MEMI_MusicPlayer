package com.mmc.merimusicplayer.data.model.youtube.playback

data class YouTubeHlsAudioPlaylist(
    val uri: String,
    val contentLength: Long? = null,
    val estimatedBitrate: Int = 0,
    val audioItag: Int? = null
)
