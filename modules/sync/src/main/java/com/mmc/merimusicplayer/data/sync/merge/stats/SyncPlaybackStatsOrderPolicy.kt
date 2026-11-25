package com.mmc.merimusicplayer.data.sync.merge.stats

import com.mmc.merimusicplayer.data.model.sync.SyncPlaybackStatBucket
import com.mmc.merimusicplayer.data.model.sync.SyncTrackStat

internal object SyncPlaybackStatsOrderPolicy {
    private val statOrder = compareBy(SyncTrackStat::identityKey)
    private val bucketOrder = compareBy(SyncPlaybackStatBucket::dayStartAt)
        .thenBy(SyncPlaybackStatBucket::identityKey)

    fun stats(stats: List<SyncTrackStat>): List<SyncTrackStat> = stats.sortedWhenNeeded(statOrder)

    fun buckets(buckets: List<SyncPlaybackStatBucket>): List<SyncPlaybackStatBucket> = buckets.sortedWhenNeeded(bucketOrder)

    private fun <T> List<T>.sortedWhenNeeded(order: Comparator<T>): List<T> {
        if (size < 2) return this
        val entries = iterator()
        var previous = entries.next()
        while (entries.hasNext()) {
            val current = entries.next()
            // 设备间的统计键顺序一致后，小改动才能复用相同内容块
            if (order.compare(previous, current) > 0) return sortedWith(order)
            previous = current
        }
        return this
    }
}
