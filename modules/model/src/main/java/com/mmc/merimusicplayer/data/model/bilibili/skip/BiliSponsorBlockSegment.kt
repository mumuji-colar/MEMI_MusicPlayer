package com.mmc.merimusicplayer.data.model.bilibili.skip

data class BiliSponsorBlockSegment(
    val uuid: String,
    val category: String,
    val startMs: Long,
    val endMs: Long
)
