package com.mmc.merimusicplayer.data.auth.youtube

import com.mmc.merimusicplayer.platform.youtube.auth.YouTubeAuthStore

interface YouTubeRotationHost {
    fun isInitialized(): Boolean
    suspend fun isYouTubeEnabled(): Boolean
    val authStore: YouTubeAuthStore
}

object YouTubeRotationHosts {
    @Volatile
    var current: YouTubeRotationHost? = null
        private set

    fun bind(host: YouTubeRotationHost) {
        current = host
    }
}
