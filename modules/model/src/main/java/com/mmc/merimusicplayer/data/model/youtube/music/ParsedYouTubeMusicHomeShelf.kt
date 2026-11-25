package com.mmc.merimusicplayer.data.model.youtube.music

data class ParsedYouTubeMusicHomeShelf(
    val title: String,
    val items: List<YouTubeMusicHomeItem>,
    val continuation: String? = null
)
