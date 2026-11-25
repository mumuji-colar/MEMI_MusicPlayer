package com.mmc.merimusicplayer.core.download.host

import com.mmc.merimusicplayer.data.model.youtube.auth.YouTubeAuthBundle

interface DownloadCredentials {
    fun biliCookies(): Map<String, String>
    fun youtubeAuth(): YouTubeAuthBundle
}
