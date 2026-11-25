@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mmc.merimusicplayer.data.sync.mapping

import com.mmc.merimusicplayer.data.model.sync.SyncBiliVideoSkipInterval
import com.mmc.merimusicplayer.data.model.sync.SyncBiliVideoSkipRule

import com.mmc.merimusicplayer.data.model.bilibili.skip.BiliVideoSkipInterval
import com.mmc.merimusicplayer.data.model.bilibili.skip.BiliVideoSkipRule
import com.mmc.merimusicplayer.platform.bilibili.skip.policy.normalizeBiliVideoSkipIntervals

internal fun BiliVideoSkipRule.toSyncBiliVideoSkipRule(): SyncBiliVideoSkipRule {
    return SyncBiliVideoSkipRule(
        bvid = target.bvid,
        cid = target.cid,
        intervals = intervals.map { interval ->
            SyncBiliVideoSkipInterval(interval.startMs, interval.endMs)
        },
        modifiedAt = modifiedAt,
        isDeleted = isDeleted
    )
}

internal fun SyncBiliVideoSkipRule.toBiliVideoSkipRuleOrNull(): BiliVideoSkipRule? {
    val target = targetOrNull() ?: return null
    val intervals = normalizeBiliVideoSkipIntervals(
        intervals.map { interval -> BiliVideoSkipInterval(interval.startMs, interval.endMs) }
    )
    if (!isDeleted && intervals.isEmpty()) return null
    return BiliVideoSkipRule(
        target = target,
        intervals = if (isDeleted) emptyList() else intervals,
        modifiedAt = modifiedAt.coerceAtLeast(0L),
        isDeleted = isDeleted
    )
}
