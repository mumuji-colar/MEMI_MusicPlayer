package com.mmc.merimusicplayer.platform.bilibili.api.auth

fun interface BiliCookieSource {
    fun getCookiesOnce(): Map<String, String>
}
