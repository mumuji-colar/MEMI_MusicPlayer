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
 * File: moe.ouom.neriplayer.platform.youtube.api.protocol/YouTubeMvStreamParser
 */

package moe.ouom.neriplayer.platform.youtube.api.protocol

import java.util.Locale
import moe.ouom.neriplayer.platform.youtube.api.challenge.YouTubeStreamingCipherResolver
import org.json.JSONObject

/** 解析出来的单条视频轨，[url] 已带签名，可直接拉流。 */
internal data class YouTubeResolvedVideoTrack(
    val height: Int,
    val url: String,
    val codecLabel: String
)

/**
 * 从 player 响应里取 MV 用的视频轨。
 *
 * 音乐播放只关心音频格式，MV 要的是 adaptiveFormats 里的视频轨 + 一条音轨。
 * 签名解算与 `n` 限速参数复用音频那条链路，避免出现「音频能放、画面 403」的分裂。
 */
internal object YouTubeMvStreamParser {

    /** 再高也没意义：手机解码和带宽都吃不下，1080P 封顶。 */
    private const val MAX_HEIGHT = 1080

    /** 默认给 4 档，够覆盖 1080/720/480/360 这类常见组合。 */
    private const val DEFAULT_MAX_VARIANTS = 4

    private const val AVC_CODEC = "avc1"

    private data class VideoCandidate(
        val format: JSONObject,
        val height: Int,
        val codec: String,
        val mimeType: String,
        val bitrate: Int
    )

    private data class AudioCandidate(
        val format: JSONObject,
        val mimeType: String,
        val bitrate: Int
    )

    /**
     * 按分辨率分组挑轨：同一高度优先 H.264（Android 硬解覆盖最广），
     * 同编码内取码率最高的一条。
     */
    suspend fun resolveVideoTracks(
        root: JSONObject,
        cipherResolver: YouTubeStreamingCipherResolver?,
        maxVariants: Int = DEFAULT_MAX_VARIANTS
    ): List<YouTubeResolvedVideoTrack> {
        val candidates = collectVideoCandidates(root)
        if (candidates.isEmpty()) return emptyList()
        val picked = candidates
            .groupBy { it.height }
            .map { (height, group) ->
                val avc = group.filter { it.codec == AVC_CODEC }
                height to (avc.ifEmpty { group }).maxByOrNull { it.bitrate }
            }
            .filter { it.second != null }
            .sortedByDescending { it.first }
            .take(maxVariants.coerceAtLeast(1))

        val tracks = ArrayList<YouTubeResolvedVideoTrack>(picked.size)
        for ((height, candidate) in picked) {
            val format = candidate?.format ?: continue
            val url = YouTubeMusicPlaybackParser.resolveFormatUrlAsync(format, cipherResolver)
            if (url.isBlank()) continue
            tracks += YouTubeResolvedVideoTrack(
                height = height,
                url = url,
                codecLabel = codecLabel(candidate.codec)
            )
        }
        return tracks
    }

    /** 音轨优先 m4a/AAC：与 H.264 视频轨是同一套最稳的组合。 */
    suspend fun resolveAudioUrl(
        root: JSONObject,
        cipherResolver: YouTubeStreamingCipherResolver?
    ): String? {
        val candidates = collectAudioCandidates(root)
        if (candidates.isEmpty()) return null
        val sorted = candidates.sortedWith(
            compareByDescending<AudioCandidate> { isM4a(it.mimeType) }
                .thenByDescending { it.bitrate }
        )
        for (candidate in sorted) {
            val url = YouTubeMusicPlaybackParser.resolveFormatUrlAsync(candidate.format, cipherResolver)
            if (url.isNotBlank()) return url
        }
        return null
    }

    /**
     * 兜底：极少数响应只给 progressive（音画合一）流，
     * 这时不需要再拼音轨，直接当单档位播放。
     */
    suspend fun resolveMuxedTrack(
        root: JSONObject,
        cipherResolver: YouTubeStreamingCipherResolver?
    ): YouTubeResolvedVideoTrack? {
        val streamingData = root.optJSONObject("streamingData") ?: return null
        val formats = streamingData.optJSONArray("formats") ?: return null
        val candidates = (0 until formats.length()).mapNotNull { index ->
            val format = formats.optJSONObject(index) ?: return@mapNotNull null
            val mimeType = normalizeMimeType(format.optString("mimeType")) ?: return@mapNotNull null
            if (!mimeType.startsWith("video/")) return@mapNotNull null
            VideoCandidate(
                format = format,
                height = format.optInt("height", 0),
                codec = codecOf(format.optString("mimeType")),
                mimeType = mimeType,
                bitrate = format.optInt("bitrate", 0)
            )
        }.sortedWith(
            compareByDescending<VideoCandidate> { it.height }
                .thenByDescending { it.bitrate }
        )
        for (candidate in candidates) {
            val url = YouTubeMusicPlaybackParser.resolveFormatUrlAsync(candidate.format, cipherResolver)
            if (url.isBlank()) continue
            return YouTubeResolvedVideoTrack(
                height = candidate.height,
                url = url,
                codecLabel = codecLabel(candidate.codec)
            )
        }
        return null
    }

    fun parseTitle(root: JSONObject): String =
        root.optJSONObject("videoDetails")?.optString("title").orEmpty().trim()

    fun parseDurationMs(root: JSONObject): Long {
        val raw = root.optJSONObject("videoDetails")?.opt("lengthSeconds")
        val seconds = when (raw) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull() ?: 0L
            else -> 0L
        }
        return if (seconds > 0L) seconds * 1000L else 0L
    }

    private fun collectVideoCandidates(root: JSONObject): List<VideoCandidate> {
        val adaptive = root.optJSONObject("streamingData")
            ?.optJSONArray("adaptiveFormats")
            ?: return emptyList()
        val result = ArrayList<VideoCandidate>(adaptive.length())
        for (index in 0 until adaptive.length()) {
            val format = adaptive.optJSONObject(index) ?: continue
            val mimeType = normalizeMimeType(format.optString("mimeType")) ?: continue
            if (!mimeType.startsWith("video/")) continue
            val height = format.optInt("height", 0)
            if (height <= 0 || height > MAX_HEIGHT) continue
            result += VideoCandidate(
                format = format,
                height = height,
                codec = codecOf(format.optString("mimeType")),
                mimeType = mimeType,
                bitrate = format.optInt("bitrate", 0)
            )
        }
        return result
    }

    private fun collectAudioCandidates(root: JSONObject): List<AudioCandidate> {
        val streamingData = root.optJSONObject("streamingData") ?: return emptyList()
        val adaptive = streamingData.optJSONArray("adaptiveFormats") ?: return emptyList()
        val result = ArrayList<AudioCandidate>(adaptive.length())
        for (index in 0 until adaptive.length()) {
            val format = adaptive.optJSONObject(index) ?: continue
            val mimeType = normalizeMimeType(format.optString("mimeType")) ?: continue
            if (!mimeType.startsWith("audio/")) continue
            result += AudioCandidate(
                format = format,
                mimeType = mimeType,
                bitrate = format.optInt("bitrate", 0)
            )
        }
        return result
    }

    private fun normalizeMimeType(rawMimeType: String): String? =
        rawMimeType.substringBefore(';').trim().takeIf { it.isNotBlank() }

    private fun isM4a(mimeType: String): Boolean =
        mimeType.lowercase(Locale.US) in M4A_MIME_TYPES

    private val M4A_MIME_TYPES = setOf("audio/mp4", "audio/m4a", "audio/aac")

    /** mimeType 形如 `video/mp4; codecs="avc1.640028"`，只留编码族名。 */
    private fun codecOf(rawMimeType: String): String {
        val codecs = rawMimeType.substringAfter("codecs=", "")
            .trim()
            .removeSurrounding("\"")
            .substringBefore(',')
            .substringBefore('.')
            .trim()
            .lowercase(Locale.US)
        return codecs
    }

    private fun codecLabel(codec: String): String = when (codec) {
        "avc1", "avc3" -> "H.264"
        "hvc1", "hev1" -> "H.265"
        "av01" -> "AV1"
        "vp9", "vp09" -> "VP9"
        else -> codec.ifBlank { "MP4" }.uppercase(Locale.US)
    }
}
