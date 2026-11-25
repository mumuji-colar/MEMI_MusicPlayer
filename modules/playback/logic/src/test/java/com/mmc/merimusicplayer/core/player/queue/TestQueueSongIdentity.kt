package com.mmc.merimusicplayer.core.player.queue
import com.mmc.merimusicplayer.core.player.queue.identity.QueueSongIdentity
import com.mmc.merimusicplayer.data.model.SongItem

internal object TestQueueSongIdentity : QueueSongIdentity {
    override fun stableKey(song: SongItem): String = song.id.toString()

    override fun sameIdentity(song: SongItem, other: SongItem?): Boolean = song.id == other?.id
}
