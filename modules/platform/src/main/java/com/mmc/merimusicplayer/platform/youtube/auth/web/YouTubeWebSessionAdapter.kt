package com.mmc.merimusicplayer.platform.youtube.auth.web

import android.webkit.CookieManager
import com.mmc.merimusicplayer.platform.youtube.api.auth.YouTubeCookieSupport
import com.mmc.merimusicplayer.platform.youtube.api.potoken.YouTubeWebSession
import com.mmc.merimusicplayer.network.weblogin.ForegroundWebLoginGuard

internal object YouTubeWebSessionAdapter : YouTubeWebSession {
    override val isForegroundLoginActive: Boolean
        get() = ForegroundWebLoginGuard.isActive

    override val foregroundLoginSkipReason: String
        get() = ForegroundWebLoginGuard.SKIP_REASON

    override fun applyAuthCookies(cookieManager: CookieManager, cookies: Map<String, String>) {
        applyYouTubeWebCookies(
            cookieManager = cookieManager,
            cookies = cookies,
            urls = YouTubeCookieSupport.webCookieReadUrls,
            skipExisting = true,
            includeConsentCookie = true
        )
    }
}
