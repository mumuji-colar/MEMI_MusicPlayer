package com.mmc.merimusicplayer.data.model.netease.auth

data class NeteaseYdDeviceSnapshot(
    val token: String = "",
    val sDeviceId: String = "",
    val cookies: Map<String, String> = emptyMap()
)
