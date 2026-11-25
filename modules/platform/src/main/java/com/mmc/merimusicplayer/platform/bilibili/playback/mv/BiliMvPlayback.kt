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
 * File: com.mmc.merimusicplayer.platform.bilibili.playback.mv/BiliMvPlayback
 */

package com.mmc.merimusicplayer.platform.bilibili.playback.mv

import java.io.IOException
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.bilibili.playback.DashStream
import com.mmc.merimusicplayer.data.model.bilibili.playback.PlayOptions
import com.mmc.merimusicplayer.data.model.mv.MvPlatform
import com.mmc.merimusicplayer.data.model.mv.MvStream
import com.mmc.merimusicplayer.data.model.mv.MvVariant
import com.mmc.merimusicplayer.data.model.mv.MvTarget
import com.mmc.merimusicplayer.platform.bilibili.api.client.BiliClient
import com.mmc.merimusicplayer.platform.bilibili.playback.resolver.biliBvidOrNull
import com.mmc.merimusicplayer.platform.bilibili.playback.resolver.biliCidOrNull

private const val AVC_CODEC_ID = 7
private const val MAX_HEIGHT = 1080
private const val QN_1080P = 80

/** B 站 CDN 按 UA 校验：带 "Mobile" 的 UA 会被 openresty 直接 403。 */
internal const val BILI_MV_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Safari/537.36"

/** 拉 B 站流必须带的请求头。 */
val BILI_MV_HEADERS: Map<String, String> = mapOf(
    "User-Agent" to BILI_MV_USER_AGENT,
    "Referer" to "https://www.bilibili.com/"
)

/**
 * B 站音源的歌本身就是一个视频，所以「有没有 MV」等价于「能不能解析出 bvid」。
 */
fun SongItem.biliMvTargetOrNull(): MvTarget? {
    val bvid = biliBvidOrNull() ?: return null
    return MvTarget(
        platform = MvPlatform.BILIBILI,
        videoId = bvid,
        cid = biliCidOrNull(),
        title = name,
        artist = artist,
        coverUrl = coverUrl
    )
}

suspend fun resolveBiliMvStream(client: BiliClient, target: MvTarget): MvStream {
    val bvid = target.videoId.trim()
    if (bvid.isEmpty()) throw IOException("缺少 BV 号")

    val cid = target.cid ?: run {
        val basic = client.getVideoBasicInfoByBvid(bvid)
        basic.pages.firstOrNull()?.cid ?: throw IOException("这个视频没有可播放的分 P")
    }

    val info = client.getPlayInfoByBvid(
        bvid = bvid,
        cid = cid,
        opts = PlayOptions(qn = QN_1080P, fourk = 1)
    )
    if (info.code != 0) throw IOException("取流失败：${info.message}")

    val video = selectMvVideoStream(info.dashVideo)
    if (video != null) {
        val variants = buildBiliVariants(info.dashVideo, info.dashAudio)
        if (variants.isNotEmpty()) {
            return MvStream(
                title = target.title.ifBlank { bvid },
                variants = variants,
                durationMs = info.timeLengthMs,
                headers = BILI_MV_HEADERS
            )
        }
    }

    // 少数老视频只有 durl，音画已经合在一起
    val progressive = info.durl.firstOrNull() ?: throw IOException("没有可用的视频流")
    return MvStream(
        title = target.title.ifBlank { bvid },
        variants = listOf(
            MvVariant(
                height = 0,
                label = "原始",
                videoUrls = listOf(progressive.url) + progressive.backupUrls,
                codecLabel = "MP4"
            )
        ),
        durationMs = progressive.lengthMs,
        headers = BILI_MV_HEADERS
    )
}

/**
 * 把 dash 里的视频流按分辨率分组，每组取一条可播的（优先 H.264），
 * 这样切清晰度是本地换流，不用再打一次 playurl。
 */
internal fun buildBiliVariants(
    videos: List<DashStream>,
    audios: List<DashStream>
): List<MvVariant> {
    if (videos.isEmpty()) return emptyList()
    val audio = selectMvAudioStream(audios)
    val audioUrls = audio?.let { listOf(it.baseUrl) + it.backupUrls }.orEmpty()
    return videos
        .filter { it.height > 0 }
        .groupBy { it.height }
        .map { (height, group) -> height to pickVideoForHeight(group) }
        .sortedByDescending { it.first }
        .map { (height, stream) ->
            MvVariant(
                height = height,
                label = "${height}P",
                videoUrls = listOf(stream.baseUrl) + stream.backupUrls,
                audioUrls = audioUrls,
                codecLabel = mvCodecLabel(stream.codecid)
            )
        }
}

private fun pickVideoForHeight(group: List<DashStream>): DashStream {
    val avc = group.filter { it.codecid == AVC_CODEC_ID }
    return (avc.ifEmpty { group }).maxByOrNull { it.bandwidth } ?: group.first()
}

/**
 * 优先 H.264：Android 设备对 AV1/HEVC 的硬解支持参差不齐，能播比清晰更重要。
 * 同编码内取不高于 1080p 的最高分辨率，再取更高码率。
 */
internal fun selectMvVideoStream(streams: List<DashStream>): DashStream? {
    val candidates = streams.filter { it.height in 1..MAX_HEIGHT }
    if (candidates.isEmpty()) return null
    val avc = candidates.filter { it.codecid == AVC_CODEC_ID }
    val pool = avc.ifEmpty { candidates }
    return pool.maxWithOrNull(
        compareBy<DashStream> { it.height }.thenBy { it.bandwidth }
    )
}

/** 音轨只在同带宽下比较，尽量选码率最高的那条。 */
internal fun selectMvAudioStream(streams: List<DashStream>): DashStream? =
    streams.maxByOrNull { it.bandwidth }

private fun mvCodecLabel(codecid: Int): String = when (codecid) {
    7 -> "H.264"
    12 -> "H.265"
    13 -> "AV1"
    else -> "codec$codecid"
}
