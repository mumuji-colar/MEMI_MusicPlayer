package com.mmc.merimusicplayer.platform.lyrics.search

import com.mmc.merimusicplayer.data.model.music.SongSearchInfo

internal data class SearchCandidateScore(
    val candidate: SongSearchInfo,
    val score: Int,
    val durationDeltaMs: Long
)
