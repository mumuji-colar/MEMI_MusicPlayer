package com.mmc.merimusicplayer.core.player.session

import com.mmc.merimusicplayer.data.identity.sameIdentityAs
import com.mmc.merimusicplayer.data.identity.stableKey

import com.mmc.merimusicplayer.core.player.queue.identity.QueueSongIdentity
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.stableKey

internal object AppQueueSongIdentity : QueueSongIdentity {
    override fun stableKey(song: SongItem): String = song.stableKey()

    override fun sameIdentity(song: SongItem, other: SongItem?): Boolean = song.sameIdentityAs(other)
}
