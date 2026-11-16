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
 * File: moe.ouom.neriplayer.platform.netease.mv/NeteaseMvPlayback
 */

package moe.ouom.neriplayer.platform.netease.mv

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.model.mv.MvPlatform
import moe.ouom.neriplayer.data.model.mv.MvStream
import moe.ouom.neriplayer.data.model.mv.MvTarget
import moe.ouom.neriplayer.data.model.netease.crypto.CryptoMode
import moe.ouom.neriplayer.platform.netease.api.client.NeteaseClient
import org.json.JSONObject

/** 网易云 MV 走自己的 CDN，认 Referer，UA 用桌面端更稳。 */
internal const val NETEASE_MV_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Safari/537.36"

val NETEASE_MV_HEADERS: Map<String, String> = mapOf(
    "User-Agent" to NETEASE_MV_USER_AGENT,
    "Referer" to "https://music.163.com/"
)

/** 网易云的 MV 是歌曲详情里的 `mv` 字段，0 表示没有 MV。 */
fun SongItem.neteaseMvTargetOrNull(mvId: Long): MvTarget? {
    if (!channelId.equals("netease", ignoreCase = true)) return null
    if (mvId <= 0L) return null
    return MvTarget(
        platform = MvPlatform.NETEASE,
        videoId = mvId.toString(),
        title = name,
        artist = artist,
        coverUrl = coverUrl
    )
}

/**
 * 歌曲 id → MV id 的查询缓存。
 *
 * 播放页一次只查一首，但歌单里每行都要判断有没有 MV，
 * 所以请求会先进队列，攒 ~60ms 合成一次 `v3/song/detail` 批量查询，
 * 一屏 50 首也只是 1 个请求。查不到（`mv=0`）也要缓存，避免反复问。
 */
class NeteaseMvRepository(
    private val client: NeteaseClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private data class Ask(val songId: Long, val result: CompletableDeferred<Long>)

    private val cache = ConcurrentHashMap<Long, Long>()
    private val queue = Channel<Ask>(Channel.UNLIMITED)
    private val consumerStarted = AtomicBoolean(false)

    /** 命中缓存直接返回；未命中就排队批量查，超时按「没有 MV」处理。 */
    suspend fun mvIdOf(songId: Long): Long? {
        if (songId <= 0L) return null
        cache[songId]?.let { cached -> return cached.takeIf { it > 0L } }
        startConsumer()
        val ask = Ask(songId, CompletableDeferred())
        queue.send(ask)
        return withTimeoutOrNull(LOOKUP_TIMEOUT_MS) { ask.result.await() }?.takeIf { it > 0L }
    }

    fun cachedMvId(songId: Long): Long? = cache[songId]?.takeIf { it > 0L }

    fun putMvId(songId: Long, mvId: Long) {
        if (songId > 0L) cache[songId] = mvId
    }

    private fun startConsumer() {
        if (consumerStarted.compareAndSet(false, true)) {
            scope.launch { consume() }
        }
    }

    private suspend fun consume() {
        while (true) {
            val first = queue.receive()
            val batch = mutableListOf(first)
            // 用 tryReceive 轮询而不是可取消的 receive：取消正在等待的 receive 有丢元素的风险
            val deadline = System.currentTimeMillis() + BATCH_WINDOW_MS
            while (batch.size < MAX_BATCH) {
                val next = queue.tryReceive().getOrNull()
                if (next != null) {
                    batch += next
                    continue
                }
                if (System.currentTimeMillis() >= deadline) break
                delay(BATCH_POLL_MS)
            }
            val ids = batch.map { it.songId }.distinct()
            val resolved = runCatching {
                withContext(Dispatchers.IO) { fetchMvIds(ids) }
            }
            resolved.onSuccess { mvById ->
                mvById.forEach { (id, mvId) -> cache[id] = mvId }
                batch.forEach { ask -> ask.result.complete(mvById[ask.songId] ?: 0L) }
            }.onFailure {
                // 失败不写缓存，下次进入再试
                batch.forEach { ask -> ask.result.complete(0L) }
            }
        }
    }

    private fun fetchMvIds(ids: List<Long>): Map<Long, Long> {
        val raw = client.getSongDetail(ids)
        val songs = JSONObject(raw).optJSONArray("songs") ?: return emptyMap()
        val result = HashMap<Long, Long>(songs.length())
        for (index in 0 until songs.length()) {
            val song = songs.optJSONObject(index) ?: continue
            val id = song.optLong("id", 0L)
            if (id <= 0L) continue
            result[id] = song.optLong("mv", 0L)
        }
        return result
    }

    private companion object {
        const val MAX_BATCH = 100
        const val BATCH_WINDOW_MS = 60L
        const val BATCH_POLL_MS = 10L
        const val LOOKUP_TIMEOUT_MS = 12_000L
    }
}

/**
 * 取网易云 MV 的播放地址。
 *
 * 老的 `mv/url` 接口已经下线（任何清晰度档位都返回 `code=404 接口未找到`），
 * 现在地址直接放在 `mv/detail` 的 `brs` 字段里：档位 → 直链，
 * 未登录一般给 240/480，会员账号会多出 720/1080，所以取最高档即可。
 *
 * 拿到的直链是带 `wsSecret/wsTime` 的签名地址，音画合一（mp4/H.264），
 * CDN 不校验 Referer，但带上更稳。
 */
suspend fun resolveNeteaseMvStream(client: NeteaseClient, target: MvTarget): MvStream {
    val mvid = target.videoId.trim().toLongOrNull() ?: throw IOException("缺少 MV 编号")
    val raw = withContext(Dispatchers.IO) {
        client.request(
            url = "https://music.163.com/api/mv/detail",
            params = mapOf("id" to mvid.toString()),
            mode = CryptoMode.API,
            method = "GET"
        )
    }
    if (raw.isBlank()) throw IOException("MV 详情无响应")
    val json = runCatching { JSONObject(raw) }
        .getOrElse { throw IOException("MV 详情无法解析") }
    val code = json.optInt("code", -1)
    if (code != 200) throw IOException("MV 详情请求失败（code=$code）")
    val data = json.optJSONObject("data") ?: throw IOException("MV 不存在或已下架")

    val brs = data.optJSONObject("brs")
    val picked = brs?.keys()?.asSequence()
        ?.mapNotNull { key ->
            val url = brs.optString(key).orEmpty()
            val quality = key.toIntOrNull()
            if (quality == null || url.isBlank()) null else quality to url
        }
        ?.maxByOrNull { it.first }
    if (picked == null) {
        throw IOException("这首 MV 没有可用地址（可能需要会员或已下架）")
    }

    return MvStream(
        title = target.title.ifBlank { data.optString("name") },
        videoUrls = listOf(picked.second.toHttpsUrl()),
        audioUrls = emptyList(),
        codecLabel = "MP4 · ${picked.first}P",
        durationMs = data.optLong("duration", 0L).takeIf { it > 0L },
        headers = NETEASE_MV_HEADERS
    )
}

private fun String.toHttpsUrl(): String = replaceFirst("http://", "https://")
