package com.mmc.merimusicplayer.data.sync.merge.policy

import com.mmc.merimusicplayer.data.model.stats.LocalPlaylistPlayBucket
import com.mmc.merimusicplayer.data.model.stats.LocalPlaylistPlaybackStat
import com.mmc.merimusicplayer.data.playlist.usage.localPlaylistHotEntriesForPeriod
import com.mmc.merimusicplayer.data.model.stats.PlaybackStatsPeriod
import com.mmc.merimusicplayer.data.model.stats.resolvePlaybackStatsTimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPlaylistUsageDisplayTest {
    @Test
    fun `weekly and monthly hot playlists use daily buckets and stable ordering`() {
        val now = 1_700_000_000_000L
        val weekStart = PlaybackStatsPeriod.WEEK.resolvePlaybackStatsTimeRange(now)
            .startInclusive!!
        val monthStart = PlaybackStatsPeriod.MONTH.resolvePlaybackStatsTimeRange(now)
            .startInclusive!!
        val stats = listOf(
            LocalPlaylistPlaybackStat(
                playlistId = 1L,
                dailyPlayBuckets = listOf(LocalPlaylistPlayBucket(weekStart, playCount = 3L))
            ),
            LocalPlaylistPlaybackStat(
                playlistId = 2L,
                dailyPlayBuckets = listOf(LocalPlaylistPlayBucket(weekStart, playCount = 5L))
            ),
            LocalPlaylistPlaybackStat(
                playlistId = 3L,
                dailyPlayBuckets = listOf(LocalPlaylistPlayBucket(monthStart, playCount = 9L))
            )
        )

        val weekly = localPlaylistHotEntriesForPeriod(
            stats = stats,
            period = PlaybackStatsPeriod.WEEK,
            nowMillis = now
        )
        val monthly = localPlaylistHotEntriesForPeriod(
            stats = stats,
            period = PlaybackStatsPeriod.MONTH,
            nowMillis = now
        )

        assertEquals(listOf(2L, 1L), weekly.map { it.playlistId })
        assertEquals(listOf(5L, 3L), weekly.map { it.playCount })
        assertEquals(listOf(3L, 2L, 1L), monthly.map { it.playlistId })
        assertTrue(monthly.all { it.playCount > 0L })
    }

}
