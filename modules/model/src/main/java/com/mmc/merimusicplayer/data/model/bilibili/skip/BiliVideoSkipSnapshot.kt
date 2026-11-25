package com.mmc.merimusicplayer.data.model.bilibili.skip

data class BiliVideoSkipSnapshot(
    val rules: List<BiliVideoSkipRule>,
    val drafts: List<BiliVideoSkipDraft>
)
