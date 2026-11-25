package com.mmc.merimusicplayer.platform.youtube.auth

import com.mmc.merimusicplayer.platform.youtube.api.auth.hasLoginCookies
import com.mmc.merimusicplayer.platform.youtube.api.auth.normalized

import com.mmc.merimusicplayer.platform.youtube.api.bootstrap.YouTubeBootstrapHtmlSource
import com.mmc.merimusicplayer.data.model.youtube.auth.YOUTUBE_MUSIC_ORIGIN
import com.mmc.merimusicplayer.data.model.youtube.auth.YouTubeAuthBundle
import com.mmc.merimusicplayer.platform.youtube.api.transport.YOUTUBE_WEB_ORIGIN
import com.mmc.merimusicplayer.platform.youtube.api.transport.buildYouTubePageRequestHeaders
import com.mmc.merimusicplayer.platform.youtube.api.transport.resolveBootstrapUserAgent
import com.mmc.merimusicplayer.data.model.youtube.auth.YouTubeBootstrapSessionState
import okhttp3.Request

internal fun parseYouTubeBootstrapSessionState(
    html: String,
    origin: String = ""
): YouTubeBootstrapSessionState {
    val bootstrapSource = YouTubeBootstrapHtmlSource(html)
    return YouTubeBootstrapSessionState(
        origin = origin,
        loggedIn = bootstrapSource.optionalBoolean("LOGGED_IN")
            .equals("true", ignoreCase = true),
        sessionIndex = bootstrapSource.optionalString("SESSION_INDEX"),
        delegatedSessionId = bootstrapSource.optionalString("DELEGATED_SESSION_ID"),
        userSessionId = bootstrapSource.optionalString("USER_SESSION_ID")
    )
}

class YouTubeWebLoginVerifier(
    private val executeText: (Request) -> String,
    private val pageOrigins: List<String> = listOf(
        YOUTUBE_MUSIC_ORIGIN,
        YOUTUBE_WEB_ORIGIN
    )
) {
    fun verifyBlocking(bundle: YouTubeAuthBundle): YouTubeBootstrapSessionState {
        val normalizedBundle = bundle.normalized(
            savedAt = bundle.savedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
        if (!normalizedBundle.hasLoginCookies()) {
            return YouTubeBootstrapSessionState()
        }

        val requestHeaders = normalizedBundle.buildYouTubePageRequestHeaders(
            original = linkedMapOf(
                "Cache-Control" to "no-cache",
                "Pragma" to "no-cache"
            ),
            userAgent = normalizedBundle.resolveBootstrapUserAgent(),
            includeAuthUser = normalizedBundle.xGoogAuthUser.isNotBlank()
        )

        var lastObservedState = YouTubeBootstrapSessionState()
        pageOrigins.forEach { origin ->
            val request = Request.Builder()
                .url("$origin/")
                .apply {
                    requestHeaders.forEach { (name, value) ->
                        header(name, value)
                    }
                }
                .build()
            val html = runCatching { executeText(request) }.getOrNull() ?: return@forEach
            val sessionState = parseYouTubeBootstrapSessionState(
                html = html,
                origin = origin
            )
            lastObservedState = sessionState
            if (sessionState.hasLiveSessionSignal()) {
                return sessionState
            }
        }
        return lastObservedState
    }
}
