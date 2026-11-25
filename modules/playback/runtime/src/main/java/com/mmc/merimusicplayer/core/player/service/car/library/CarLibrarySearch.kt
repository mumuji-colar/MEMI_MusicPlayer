package com.mmc.merimusicplayer.core.player.service.car.library

import com.mmc.merimusicplayer.data.local.media.displayArtist
import com.mmc.merimusicplayer.data.local.media.displayName
import com.mmc.merimusicplayer.data.model.SongItem
import java.util.Locale

internal object CarLibrarySearch {
    private const val MAX_QUERY_LENGTH = 120
    private val whitespace = Regex("\\s+")

    fun normalize(query: String): String? {
        if (query.length > MAX_QUERY_LENGTH) return null
        return query.trim().replace(whitespace, " ").takeIf(String::isNotBlank)
    }

    fun matchingSongs(songs: List<SongItem>, query: String): List<SongItem> {
        val terms = query.lowercase(Locale.ROOT).split(' ')
        return songs.filter { song ->
            val text = listOf(song.displayName(), song.displayArtist(), song.album, song.name, song.artist)
                .joinToString(" ").lowercase(Locale.ROOT)
            terms.all(text::contains)
        }
    }
}
