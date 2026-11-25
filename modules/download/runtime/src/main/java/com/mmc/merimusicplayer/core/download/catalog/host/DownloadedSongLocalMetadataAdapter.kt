package com.mmc.merimusicplayer.core.download.catalog.host

import com.mmc.merimusicplayer.core.download.catalog.assembly.DownloadedSongLocalMetadata
import com.mmc.merimusicplayer.data.model.local.LocalMediaDetails

internal fun LocalMediaDetails.toSongLocalMetadata(): DownloadedSongLocalMetadata =
    DownloadedSongLocalMetadata(
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        coverUri = coverUri,
        lyricContent = lyricContent,
        originalTitle = originalTitle,
        originalArtist = originalArtist,
        sourceStableKey = sourceStableKey
    )
