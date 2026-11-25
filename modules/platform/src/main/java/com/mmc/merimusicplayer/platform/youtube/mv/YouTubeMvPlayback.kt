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
 * File: com.mmc.merimusicplayer.platform.youtube.mv/YouTubeMvPlayback
 */

package com.mmc.merimusicplayer.platform.youtube.mv

import java.io.IOException
import java.util.Locale
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.mv.MvPlatform
import com.mmc.merimusicplayer.data.model.mv.MvStream
import com.mmc.merimusicplayer.data.model.mv.MvTarget
import com.mmc.merimusicplayer.data.model.mv.MvVariant
import com.mmc.merimusicplayer.data.model.youtube.music.YouTubeMusicSearchFilter
import com.mmc.merimusicplayer.data.model.youtube.music.YouTubeMusicSearchResult
import com.mmc.merimusicplayer.platform.youtube.api.client.YouTubeMusicClient
import com.mmc.merimusicplayer.platform.youtube.api.transport.extractYouTubeMusicVideoId
import com.mmc.merimusicplayer.platform.youtube.repository.YouTubeMusicPlaybackRepository

/**
 * YouTube Music 的音源分两种：
 *
 * - `MUSIC_VIDEO_TYPE_ATV`：art track，上传时就是一张静态封面加音轨，
 *   视频轨确实存在，但画面永远定格在封面上；
 * - `MUSIC_VIDEO_TYPE_OMV/UGC`：真正有画面的 MV。
 *
 * 歌单里的歌绝大多数是前者，所以「有没有 MV」不能只看能不能取到 videoId，
 * 还要再找一次同名的视频版本，否则点开只会看到一张不动的封面。
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
 *
 * 对 art track 会先按「歌名 + 歌手」搜一次视频，用搜到的正式 MV 顶替，
 * 搜不到才退回原视频（至少声音是对的）。
 */
suspend fun resolveYouTubeMvStream(
    repository: YouTubeMusicPlaybackRepository,
    client: YouTubeMusicClient,
    target: MvTarget
): MvStream {
    val songVideoId = target.videoId.trim()
    if (songVideoId.isEmpty()) throw IOException("缺少视频编号")

    val mv = resolveMvVideo(client, target)
    val playVideoId = mv?.videoId?.takeIf { it.isNotBlank() } ?: songVideoId

    var fellBack = false
    val resolved = repository.getBestPlayableVideo(playVideoId)
        ?: if (playVideoId != songVideoId) {
            // 搜到的视频取不到流（地区限制之类），退回原视频
            fellBack = true
            repository.getBestPlayableVideo(songVideoId)
        } else {
            null
        }
        ?: throw IOException("没有可用的视频流")
    if (resolved.variants.isEmpty()) throw IOException("没有可用的视频流")

    val notice = when {
        fellBack -> "找 MV 取不到画面，已退回原视频"
        mv == null -> "没找到这首歌的 MV，播放的是静态封面版"
        else -> null
    }

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

    val title = mv?.title
        ?.takeIf { playVideoId != songVideoId && it.isNotBlank() }
        ?: target.title.ifBlank { resolved.title }.ifBlank { songVideoId }

    return MvStream(
        title = title,
        variants = variants,
        durationMs = resolved.durationMs.takeIf { it > 0L },
        headers = resolved.headers,
        notice = notice
    )
}

/** 同一次应用生命周期内记住结果，反复进出 MV 页不必重复搜。 */
private val mvLookupCache = object {
    private val entries = object : LinkedHashMap<String, YouTubeMusicSearchResult>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, YouTubeMusicSearchResult>?): Boolean =
            size > CACHE_SIZE
    }

    fun get(key: String): YouTubeMusicSearchResult? = synchronized(entries) { entries[key] }

    fun put(key: String, value: YouTubeMusicSearchResult) {
        synchronized(entries) { entries[key] = value }
    }
}

private const val CACHE_SIZE = 64

/** 视频搜索取前几条足够：结果按相关度排序，命中一般在前三。 */
private const val MV_SEARCH_LIMIT = 8

/** 歌手栏位为空时的占位，不参与匹配。 */
private val PLACEHOLDER_ARTISTS = setOf("youtube", "youtube music", "未知歌手")

/**
 * 找出这首歌对应的视频版本。
 *
 * 优先要这首歌自己（它本来就是 MV 的情况），其次按歌名匹配、歌手做加权过滤。
 */
private suspend fun resolveMvVideo(
    client: YouTubeMusicClient,
    target: MvTarget
): YouTubeMusicSearchResult? {
    val rawTitle = target.title.trim()
    if (rawTitle.isEmpty()) return null

    val artist = target.artist.trim()
    val cacheKey = "${rawTitle.lowercase(Locale.ROOT)}|${artist.lowercase(Locale.ROOT)}"
    mvLookupCache.get(cacheKey)?.let { return it }

    val query = buildString {
        append(rawTitle)
        if (artist.isNotEmpty() && artist.lowercase(Locale.ROOT) !in PLACEHOLDER_ARTISTS) {
            append(' ')
            append(artist)
        }
    }

    val results = runCatching {
        client.search(
            query = query,
            limit = MV_SEARCH_LIMIT,
            filter = YouTubeMusicSearchFilter.Video
        )
    }.getOrNull().orEmpty()
    if (results.isEmpty()) return null

    val matched = results.firstOrNull { it.videoId == target.videoId }
        ?: pickByTitle(results, rawTitle, artist)
        ?: return null

    mvLookupCache.put(cacheKey, matched)
    return matched
}

private fun pickByTitle(
    results: List<YouTubeMusicSearchResult>,
    title: String,
    artist: String
): YouTubeMusicSearchResult? {
    val normalizedTitle = normalizeForMatch(title)
    if (normalizedTitle.isEmpty()) return null
    val titleMatched = results.filter { result ->
        val candidate = normalizeForMatch(result.title)
        if (normalizedTitle.length < 2) {
            // 单字歌名放宽匹配只会挑错视频，要求完全一致
            candidate == normalizedTitle
        } else {
            candidate.contains(normalizedTitle) || normalizedTitle.contains(candidate)
        }
    }
    if (titleMatched.isEmpty()) return null

    val normalizedArtist = normalizeForMatch(artist)
        .takeIf { artist.isNotEmpty() && artist.lowercase(Locale.ROOT) !in PLACEHOLDER_ARTISTS }
    if (normalizedArtist != null) {
        val artistMatched = titleMatched.filter { result ->
            normalizeForMatch(result.artist).contains(normalizedArtist) ||
                normalizeForMatch(result.title).contains(normalizedArtist)
        }
        if (artistMatched.isNotEmpty()) return artistMatched.first()
    }
    return titleMatched.first()
}

/** 只留字母数字，抹平全半角标点与空格差异。 */
private fun normalizeForMatch(value: String): String = buildString(value.length) {
    for (char in value.lowercase(Locale.ROOT)) {
        if (char.isLetterOrDigit()) append(char)
    }
}
