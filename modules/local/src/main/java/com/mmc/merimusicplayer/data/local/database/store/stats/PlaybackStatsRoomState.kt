package com.mmc.merimusicplayer.data.local.database.store.stats

import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatBucketEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatCounterShardEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatDailyCounterShardEntity

internal data class PlaybackStatsRoomState(val revision: Long, val clearedAt: Long, val counterEpochStartedAt: Long)

internal data class PlaybackStatsDeltaRows(
    val track: PlaybackStatEntity?, val bucket: PlaybackStatBucketEntity?,
    val counter: PlaybackStatCounterShardEntity?, val dailyCounter: PlaybackStatDailyCounterShardEntity?
)
