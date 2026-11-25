package com.mmc.merimusicplayer.core.download.host

import com.mmc.merimusicplayer.platform.bilibili.api.client.BiliClient
import com.mmc.merimusicplayer.platform.netease.api.client.NeteaseClient
import com.mmc.merimusicplayer.platform.bilibili.playback.BiliPlaybackRepository
import com.mmc.merimusicplayer.platform.youtube.repository.YouTubeMusicPlaybackRepository
import com.mmc.merimusicplayer.platform.subsonic.repository.SubsonicRepository

interface DownloadSourceServices {
    val subsonicRepository: SubsonicRepository? get() = null
    val neteaseClient: NeteaseClient
    val biliClient: BiliClient
    val biliPlaybackRepository: BiliPlaybackRepository
    val youtubeMusicPlaybackRepository: YouTubeMusicPlaybackRepository
    val youtubeMusicDownloadPlaybackRepository: YouTubeMusicPlaybackRepository
}
