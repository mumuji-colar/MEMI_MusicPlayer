package com.mmc.merimusicplayer.core.player.lan.track

import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.ltw.track.ListenTogetherTrack

internal fun lanStreamSessionSong(track: ListenTogetherTrack): SongItem = SongItem(
    id = track.audioId.take(15).toLongOrNull(16) ?: track.stableKey.hashCode().toLong(),
    name = track.name,
    artist = track.artist,
    album = track.album.orEmpty(),
    albumId = 0,
    durationMs = track.durationMs,
    coverUrl = null,
    channelId = track.channelId,
    audioId = track.audioId,
    sourceStableKey = "lan-session:${track.stableKey}",
)
