package com.mmc.merimusicplayer.platform.youtube.repository

import android.content.Context
import com.mmc.merimusicplayer.platform.youtube.api.challenge.NewPipeFallbackTracker
import com.mmc.merimusicplayer.platform.youtube.api.challenge.YouTubeEjsChallengeSolver
import com.mmc.merimusicplayer.platform.youtube.api.fallback.YouTubeNewPipeFallbackStore
import com.mmc.merimusicplayer.data.model.youtube.auth.YouTubeAuthBundle
import com.mmc.merimusicplayer.platform.youtube.api.potoken.YouTubePoTokenProvider
import com.mmc.merimusicplayer.platform.youtube.api.potoken.YouTubeWebPoTokenProvider
import com.mmc.merimusicplayer.platform.youtube.auth.web.YouTubeWebSessionAdapter
import okhttp3.OkHttpClient

internal fun createPlaybackEjsSolver(
    context: Context?,
    okHttpClient: OkHttpClient
): YouTubeEjsChallengeSolver? = context?.let { YouTubeEjsChallengeSolver(it, okHttpClient) }

internal fun resolvePlaybackPoTokenProvider(
    provider: YouTubePoTokenProvider?,
    context: Context?,
    authProvider: () -> YouTubeAuthBundle
): YouTubePoTokenProvider? = provider ?: context?.let { YouTubeWebPoTokenProvider(it, authProvider, YouTubeWebSessionAdapter) }

internal fun attachPlaybackFallbackStore(context: Context?) {
    if (context == null) return
    runCatching { NewPipeFallbackTracker.attachStore(YouTubeNewPipeFallbackStore(context)) }
}
