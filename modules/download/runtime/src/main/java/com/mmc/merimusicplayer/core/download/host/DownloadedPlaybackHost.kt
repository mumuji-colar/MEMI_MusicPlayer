package com.mmc.merimusicplayer.core.download.host

import com.mmc.merimusicplayer.data.model.SongItem

interface DownloadedPlaybackHost {
    val currentSong: SongItem?
    fun playPlaylist(songs: List<SongItem>, startIndex: Int)
    fun hydrateSongMetadata(originalSong: SongItem, updatedSong: SongItem)
}
