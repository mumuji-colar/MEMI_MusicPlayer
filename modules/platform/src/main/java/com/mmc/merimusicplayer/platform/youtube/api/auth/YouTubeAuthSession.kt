package com.mmc.merimusicplayer.platform.youtube.api.auth

import com.mmc.merimusicplayer.data.model.youtube.auth.YouTubeAuthAutoRefreshResult
import com.mmc.merimusicplayer.data.model.youtube.auth.YouTubeAuthBundle
import com.mmc.merimusicplayer.data.model.youtube.auth.YouTubeAuthHealth

interface YouTubeAuthProvider {
    fun getAuthOnce(): YouTubeAuthBundle
    fun getAuthHealthOnce(): YouTubeAuthHealth
}

interface YouTubeAuthRefresher {
    suspend fun refreshIfNeeded(reason: String, force: Boolean = false): YouTubeAuthAutoRefreshResult
}
