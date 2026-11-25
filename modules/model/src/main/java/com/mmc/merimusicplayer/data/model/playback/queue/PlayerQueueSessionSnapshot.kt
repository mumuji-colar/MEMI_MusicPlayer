package com.mmc.merimusicplayer.data.model.playback.queue

data class PlayerQueueSessionSnapshot(
    val queue: PlayerQueueSnapshot = PlayerQueueSnapshot.EMPTY,
    val shuffleEnabled: Boolean = false,
    val shuffleRestore: PlayerQueueSnapshot? = null
)
