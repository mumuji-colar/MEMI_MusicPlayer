package com.mmc.merimusicplayer.core.player.queue.policy.order

import com.mmc.merimusicplayer.data.model.SongItem

internal data class PlayerQueueShuffleOrder(
    val queueIndices: List<Int>,
    val currentIndex: Int
)

internal data class PlayerQueueRestoreOrder(
    val playlist: List<SongItem>,
    val currentIndex: Int
)
