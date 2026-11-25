package com.mmc.merimusicplayer.platform.bilibili.skip.storage

import com.mmc.merimusicplayer.data.model.bilibili.skip.BiliVideoSkipSnapshot

import com.mmc.merimusicplayer.data.model.bilibili.skip.BiliVideoSkipDraft
import com.mmc.merimusicplayer.data.model.bilibili.skip.BiliVideoSkipRule

interface BiliVideoSkipStore {
    suspend fun isPrimary(): Boolean
    suspend fun readIfPrimary(): BiliVideoSkipSnapshot?
    suspend fun replaceAll(
        rules: List<BiliVideoSkipRule>,
        drafts: List<BiliVideoSkipDraft>,
        now: Long = System.currentTimeMillis()
    )
    suspend fun replaceRules(
        rules: List<BiliVideoSkipRule>,
        now: Long = System.currentTimeMillis()
    )
    suspend fun replaceDrafts(
        drafts: List<BiliVideoSkipDraft>,
        now: Long = System.currentTimeMillis()
    )
}
