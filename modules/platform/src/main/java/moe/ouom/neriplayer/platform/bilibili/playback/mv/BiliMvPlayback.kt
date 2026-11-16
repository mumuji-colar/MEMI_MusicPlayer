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
 * File: moe.ouom.neriplayer.platform.bilibili.playback.mv/BiliMvPlayback
 */

package moe.ouom.neriplayer.platform.bilibili.playback.mv

import java.io.IOException
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.model.bilibili.mv.BiliMvTarget
import moe.ouom.neriplayer.data.model.bilibili.playback.DashStream
import moe.ouom.neriplayer.data.model.bilibili.playback.PlayOptions
import moe.ouom.neriplayer.platform.bilibili.api.client.BiliClient
import moe.ouom.neriplayer.platform.bilibili.playback.resolver.biliBvidOrNull
import moe.ouom.neriplayer.platform.bilibili.playback.resolver.biliCidOrNull

/**
 * 一首歌在 MV 播放页需要的全部信息。
 *
 * [audioUrls] 为空表示走 durl（MP4 音画合一），此时不能再拼音轨。
 */
data class BiliMvStream(
    val title: String,
    val videoUrls: List<String>,
    val audioUrls: List<String>,
    val width: Int,
    val height: Int,
    val codecLabel: String,
    val durationMs: Long?
)

private const val AVC_CODEC_ID = 7
private const val MAX_HEIGHT = 1080
private const val QN_1080P = 80

/**
 * 只有 B 站音源才谈得上「有 MV」：歌曲本身就是那个视频。
 * 其他平台（网易云/QQ/YouTube）没有 BV 号，这里直接返回 null。
 */
fun SongItem.biliMvTargetOrNull(): BiliMvTarget? {
    val bvid = biliBvidOrNull() ?: return null
    return BiliMvTarget(
        bvid = bvid,
        cid = biliCidOrNull(),
        title = name,
        artist = artist
    )
}

suspend fun resolveBiliMvStream(client: BiliClient, target: BiliMvTarget): BiliMvStream {
    val bvid = target.bvid.trim()
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
        val audio = selectMvAudioStream(info.dashAudio)
        return BiliMvStream(
            title = target.title.ifBlank { bvid },
            videoUrls = listOf(video.baseUrl) + video.backupUrls,
            audioUrls = audio?.let { listOf(it.baseUrl) + it.backupUrls }.orEmpty(),
            width = video.width,
            height = video.height,
            codecLabel = mvCodecLabel(video.codecid),
            durationMs = info.timeLengthMs
        )
    }

    // 少数老视频只有 durl，音画已经合在一起
    val progressive = info.durl.firstOrNull() ?: throw IOException("没有可用的视频流")
    return BiliMvStream(
        title = target.title.ifBlank { bvid },
        videoUrls = listOf(progressive.url) + progressive.backupUrls,
        audioUrls = emptyList(),
        width = 0,
        height = 0,
        codecLabel = "MP4",
        durationMs = progressive.lengthMs
    )
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
