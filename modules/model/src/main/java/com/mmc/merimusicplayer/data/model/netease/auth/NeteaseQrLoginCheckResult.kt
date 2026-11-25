package com.mmc.merimusicplayer.data.model.netease.auth

data class NeteaseQrLoginCheckResult(
    val code: Int,
    val message: String,
    val cookies: Map<String, String> = emptyMap()
) {
    val isConfirmed: Boolean
        get() = code == 803
}
