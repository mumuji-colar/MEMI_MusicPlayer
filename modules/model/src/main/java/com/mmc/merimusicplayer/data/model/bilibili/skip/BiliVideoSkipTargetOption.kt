package com.mmc.merimusicplayer.data.model.bilibili.skip

data class BiliVideoSkipTargetOption(
    val target: BiliVideoSkipTarget,
    val label: String,
    val durationMs: Long
)
