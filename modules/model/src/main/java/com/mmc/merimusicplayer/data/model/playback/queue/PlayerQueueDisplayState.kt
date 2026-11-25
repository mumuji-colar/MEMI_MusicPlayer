package com.mmc.merimusicplayer.data.model.playback.queue
import com.mmc.merimusicplayer.data.model.SongItem

data class PlayerQueueDisplayItem(
    val queueIndex: Int,
    val song: SongItem
)

data class PlayerQueueDisplayState(
    val items: List<PlayerQueueDisplayItem>,
    val currentDisplayIndex: Int
) {
    companion object {
        val EMPTY = PlayerQueueDisplayState(
            items = emptyList(),
            currentDisplayIndex = -1
        )
    }
}
