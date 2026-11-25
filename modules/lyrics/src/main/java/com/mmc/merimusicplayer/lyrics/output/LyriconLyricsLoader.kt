package com.mmc.merimusicplayer.lyrics.output

import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.lyrics.LyricEntry
import com.mmc.merimusicplayer.data.model.settings.lyrics.LyricSourcePreference

fun interface LyriconLyricsLoader {
    suspend fun load(
        song: SongItem,
        preferredSource: LyricSourcePreference,
        publish: (List<LyricEntry>, List<LyricEntry>, LyricSourcePreference?) -> Unit,
    )
}
