package com.mmc.merimusicplayer.core.integration.download

import com.mmc.merimusicplayer.core.download.host.DownloadedPlaybackHost
import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.data.model.SongItem

internal object AndroidDownloadedPlaybackHost : DownloadedPlaybackHost {
    override val currentSong get() = PlayerManager.currentSongFlow.value

    override fun playPlaylist(songs: List<SongItem>, startIndex: Int) {
        PlayerManager.playPlaylist(songs, startIndex)
    }

    override fun hydrateSongMetadata(originalSong: SongItem, updatedSong: SongItem) {
        PlayerManager.hydrateSongMetadata(originalSong, updatedSong)
    }
}
