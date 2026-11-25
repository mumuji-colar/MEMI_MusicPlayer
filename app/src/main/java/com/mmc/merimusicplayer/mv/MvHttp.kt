/*
 * MEMI輕音 / MemiPlayer - A modified build of MemiPlayer.
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
 * File: com.mmc.merimusicplayer.mv/MvHttp
 */

package com.mmc.merimusicplayer.mv

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

private val mvHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
}

/**
 * 各平台 CDN 的校验方式不同：B 站认 UA（移动端 UA 直接 403），网易云认 Referer。
 * 具体请求头由 platform 层解析器随 MvStream 一起给出，这里只负责套上。
 */
@OptIn(UnstableApi::class)
internal fun mvDataSourceFactory(headers: Map<String, String>): OkHttpDataSource.Factory =
    OkHttpDataSource.Factory(mvHttpClient)
        .setDefaultRequestProperties(headers)
