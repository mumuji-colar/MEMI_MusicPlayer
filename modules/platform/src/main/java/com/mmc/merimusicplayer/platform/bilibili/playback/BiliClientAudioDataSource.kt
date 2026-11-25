package com.mmc.merimusicplayer.platform.bilibili.playback

import com.mmc.merimusicplayer.platform.bilibili.api.client.BiliClient
import com.mmc.merimusicplayer.data.model.bilibili.playback.BiliAudioStreamInfo
import com.mmc.merimusicplayer.data.model.bilibili.playback.PlayOptions

/**
 * 适配器: 用 BiliClient 作为音频数据源, 接到 BiliPlaybackRepository
 */
class BiliClientAudioDataSource(
    override val client: BiliClient
) : BiliAudioDataSource {
    override suspend fun fetchAudioStreams(
        bvid: String,
        cid: Long
    ): List<BiliAudioStreamInfo> {
        return client.getAllAudioStreams(
            bvid = bvid,
            cid = cid,
            opts = PlayOptions()
        )
    }
}
