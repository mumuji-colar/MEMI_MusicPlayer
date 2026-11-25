package com.mmc.merimusicplayer.platform.bilibili.skip.resolver

import com.mmc.merimusicplayer.data.model.bilibili.skip.BiliVideoSkipTargetOption

import com.mmc.merimusicplayer.platform.bilibili.api.client.BiliClient
import com.mmc.merimusicplayer.data.model.bilibili.video.VideoBasicInfo
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.platform.bilibili.playback.resolver.resolveBiliSong
import com.mmc.merimusicplayer.data.model.bilibili.skip.BiliVideoSkipTarget

suspend fun resolveBiliVideoSkipTarget(
    song: SongItem,
    client: BiliClient
): BiliVideoSkipTarget? {
    val resolved = resolveBiliSong(song, client) ?: return null
    return BiliVideoSkipTarget(
        bvid = resolved.videoInfo.bvid,
        cid = resolved.cid
    ).normalizedOrNull()
}

suspend fun resolveBiliVideoSkipTargetOptions(
    song: SongItem,
    client: BiliClient
): List<BiliVideoSkipTargetOption> {
    val resolved = resolveBiliSong(song, client) ?: return emptyList()
    return resolved.videoInfo.toBiliVideoSkipTargetOptions()
}

suspend fun resolveBiliVideoSkipTargetOptions(
    bvid: String,
    client: BiliClient
): List<BiliVideoSkipTargetOption> {
    val normalizedBvid = bvid.trim()
    if (normalizedBvid.isEmpty()) return emptyList()
    return client.getVideoBasicInfoByBvid(normalizedBvid).toBiliVideoSkipTargetOptions()
}

private fun VideoBasicInfo.toBiliVideoSkipTargetOptions(): List<BiliVideoSkipTargetOption> {
    return pages.mapNotNull { page ->
        val target = BiliVideoSkipTarget(bvid = bvid, cid = page.cid).normalizedOrNull()
            ?: return@mapNotNull null
        BiliVideoSkipTargetOption(
            target = target,
            label = page.part.trim().ifBlank { "P${page.page}" },
            durationMs = page.durationSec.toLong().coerceAtLeast(0L) * 1_000L
        )
    }
}
