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
 * File: moe.ouom.neriplayer.platform.youtube.mv/YouTubeMvPlayback
 */

package moe.ouom.neriplayer.platform.youtube.mv

import java.io.IOException
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.model.mv.MvPlatform
import moe.ouom.neriplayer.data.model.mv.MvStream
import moe.ouom.neriplayer.data.model.mv.MvTarget
import moe.ouom.neriplayer.data.model.mv.MvVariant
import moe.ouom.neriplayer.platform.youtube.api.transport.extractYouTubeMusicVideoId
import moe.ouom.neriplayer.platform.youtube.repository.YouTubeMusicPlaybackRepository

/**
 * YouTube Music 的歌本身就是一支视频，所以「有没有 MV」等价于「能不能拿到 videoId」，
 * 不需要像网易云那样额外查一次详情接口。
 */
fun SongItem.youtubeMvTargetOrNull(): MvTarget? {
    val videoId = extractYouTubeMusicVideoId(mediaUri)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return MvTarget(
        platform = MvPlatform.YOUTUBE,
        videoId = videoId,
        title = name,
        artist = artist,
        coverUrl = coverUrl
    )
}

/**
 * 取 YouTube MV 的播放地址。
 *
 * 与音乐播放共用同一套 player 请求、签名解算与 PoToken；音画分离的流在这里
 * 交成「视频轨 + 音轨」两路，播放器用 MergingMediaSource 合起来播。
 */
suspend fun resolveYouTubeMvStream(
    repository: YouTubeMusicPlaybackRepository,
    target: MvTarget
): MvStream {
    val videoId = target.videoId.trim()
    if (videoId.isEmpty()) throw IOException("缺少视频编号")

    val resolved = repository.getBestPlayableVideo(videoId)
        ?: throw IOException("没有可用的视频流")
    if (resolved.variants.isEmpty()) throw IOException("没有可用的视频流")

    val audioUrls = resolved.audioUrl?.let { listOf(it) }.orEmpty()
    val variants = resolved.variants.map { variant ->
        MvVariant(
            height = variant.height,
            label = if (variant.height > 0) "${variant.height}P" else "原始",
            videoUrls = listOf(variant.videoUrl),
            audioUrls = audioUrls,
            codecLabel = variant.codecLabel
        )
    }

    return MvStream(
        title = target.title.ifBlank { resolved.title }.ifBlank { videoId },
        variants = variants,
        durationMs = resolved.durationMs.takeIf { it > 0L },
        headers = resolved.headers
    )
}
