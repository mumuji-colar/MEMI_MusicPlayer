package com.mmc.merimusicplayer.data.model.bilibili.playback

import com.mmc.merimusicplayer.data.model.bilibili.video.VideoBasicInfo
import com.mmc.merimusicplayer.data.model.bilibili.video.VideoPage

data class ResolvedBiliSong(
    val avid: Long,
    val cid: Long,
    val videoInfo: VideoBasicInfo,
    val pageInfo: VideoPage?
)
