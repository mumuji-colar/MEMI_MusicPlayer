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
 * File: com.mmc.merimusicplayer.data.model.youtube.playback/YouTubePlayableVideo
 */

package com.mmc.merimusicplayer.data.model.youtube.playback

/**
 * 一个可播放的视频档位。
 *
 * YouTube 的 adaptive 流音画分离，[height] 为 0 表示无法判断分辨率（progressive 兜底）。
 */
data class YouTubePlayableVideoVariant(
    val height: Int,
    val videoUrl: String,
    val codecLabel: String = ""
)

/**
 * MV 播放用的视频解析结果。
 *
 * [audioUrl] 为空表示视频本身音画合一（progressive 兜底），不能再拼音轨。
 * [headers] 是拉 googlevideo 时必需的 UA / Origin / Referer，CDN 会按 client 校验。
 */
data class YouTubePlayableVideo(
    val title: String = "",
    val durationMs: Long = 0L,
    val variants: List<YouTubePlayableVideoVariant> = emptyList(),
    val audioUrl: String? = null,
    val headers: Map<String, String> = emptyMap()
)
