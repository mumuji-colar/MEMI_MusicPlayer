package com.mmc.merimusicplayer.data.traffic

import com.mmc.merimusicplayer.data.model.traffic.TrafficStatsBucket
import com.mmc.merimusicplayer.data.model.traffic.TrafficStatsSummary

import com.mmc.merimusicplayer.data.model.stats.PlaybackStatsPeriod
import com.mmc.merimusicplayer.data.model.stats.resolvePlaybackStatsTimeRange

fun aggregateTrafficStatsForPeriod(
    buckets: List<TrafficStatsBucket>,
    period: PlaybackStatsPeriod,
    nowMillis: Long = System.currentTimeMillis()
): TrafficStatsSummary {
    val range = period.resolvePlaybackStatsTimeRange(nowMillis)
    val startInclusive = range.startInclusive
    return buckets
        .asSequence()
        .filter { bucket ->
            startInclusive == null ||
                (bucket.dayStartAt >= startInclusive && bucket.dayStartAt < range.endExclusive)
        }
        .fold(TrafficStatsSummary()) { acc, bucket ->
            TrafficStatsSummary(
                wifiBytes = acc.wifiBytes + bucket.wifiBytes,
                mobileBytes = acc.mobileBytes + bucket.mobileBytes,
                roamingBytes = acc.roamingBytes + bucket.roamingBytes,
                playbackNetworkBytes = acc.playbackNetworkBytes + bucket.playbackNetworkBytes,
                downloadNetworkBytes = acc.downloadNetworkBytes + bucket.downloadNetworkBytes,
                cacheHitBytes = acc.cacheHitBytes + bucket.cacheHitBytes,
                requestCount = acc.requestCount + bucket.requestCount,
                cacheHitCount = acc.cacheHitCount + bucket.cacheHitCount
            )
        }
}
