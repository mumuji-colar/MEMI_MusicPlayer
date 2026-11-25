package com.mmc.merimusicplayer.ui.screen.playlist

import com.mmc.merimusicplayer.data.local.media.isLocalSong
import com.mmc.merimusicplayer.data.model.SongItem

internal fun playlistDownloadCandidates(songs: List<SongItem>): List<SongItem> =
    songs.filterNot(SongItem::isLocalSong)
