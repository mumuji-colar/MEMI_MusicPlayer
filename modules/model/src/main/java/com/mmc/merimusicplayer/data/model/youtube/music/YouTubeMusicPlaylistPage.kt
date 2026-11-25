package com.mmc.merimusicplayer.data.model.youtube.music

data class YouTubeMusicPlaylistPage(
    val tracks: List<YouTubeMusicPlaylistTrack>,
    val continuation: String? = null
)
