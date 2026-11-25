package com.mmc.merimusicplayer.data.model.lyrics.amll

import com.mmc.merimusicplayer.data.model.lyrics.LyricEntry

data class AmllResolvedLyrics(
    val rawLyrics: String,
    val entries: List<LyricEntry>
)
