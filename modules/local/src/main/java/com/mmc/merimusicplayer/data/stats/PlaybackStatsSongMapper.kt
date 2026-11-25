package com.mmc.merimusicplayer.data.stats

import com.mmc.merimusicplayer.data.model.stats.TrackStat

import com.mmc.merimusicplayer.data.model.SongItem

fun TrackStat.toPlaybackStatsSongItem(): SongItem = SongItem(
    id = id,
    name = name,
    artist = artist,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    coverUrl = coverUrl,
    mediaUri = localFilePath ?: mediaUri,
    localFilePath = localFilePath,
    localFileName = localFileName,
    customName = customName,
    customArtist = customArtist,
    customCoverUrl = customCoverUrl
)
