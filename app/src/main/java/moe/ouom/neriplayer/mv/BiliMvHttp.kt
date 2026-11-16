/*
 * MEMI輕音 / MemiPlayer - A modified build of NeriPlayer.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * Copyright (C) 2026 MMC
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * File: moe.ouom.neriplayer.mv/BiliMvHttp
 */

package moe.ouom.neriplayer.mv

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/**
 * B 站 CDN 会按 UA 校验，playurl 下发的地址带 `platform=pc`，
 * 用移动端 UA（含 "Mobile"）请求会被 openresty 直接 403。
 * 所有 MV 取流都必须带这个桌面 UA。
 */
internal const val BILI_CDN_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Safari/537.36"

private const val BILI_REFERER = "https://www.bilibili.com/"

private val mvHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
}

@OptIn(UnstableApi::class)
internal fun biliMvDataSourceFactory(): OkHttpDataSource.Factory =
    OkHttpDataSource.Factory(mvHttpClient)
        .setDefaultRequestProperties(
            mapOf(
                "User-Agent" to BILI_CDN_USER_AGENT,
                "Referer" to BILI_REFERER
            )
        )
