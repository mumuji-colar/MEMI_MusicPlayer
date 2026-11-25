package com.mmc.merimusicplayer.platform.youtube.api.protocol

import okhttp3.Request

data class PreparedYouTubePlayerRequest(
    val request: Request,
    val clientVersion: String,
    val webRemixOriginalUrl: String,
    val webRemixWatchUrl: String
)
