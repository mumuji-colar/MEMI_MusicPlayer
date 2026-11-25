package com.mmc.merimusicplayer.ui.screen.playlist

import com.mmc.merimusicplayer.data.identity.identity
import com.mmc.merimusicplayer.data.identity.remoteDownloadIdentityOrNull
import com.mmc.merimusicplayer.data.local.playlist.system.FavoritesPlaylist
import com.mmc.merimusicplayer.data.model.SongItem

internal class FavoriteSongLookup(songs: List<SongItem>) {
    private val identities = SongIdentityLookup(songs)
    private val candidatesById = HashMap<Long, MutableList<SongItem>>()

    init {
        songs.forEach { song ->
            favoriteCandidateIds(song).forEach { id ->
                candidatesById.getOrPut(id) { mutableListOf() }.add(song)
            }
        }
    }

    fun contains(song: SongItem): Boolean {
        if (identities.contains(song)) return true
        return favoriteCandidateIds(song).any { id ->
            candidatesById[id]?.any { FavoritesPlaylist.matchesSong(it, song) } == true
        }
    }
}

private fun favoriteCandidateIds(song: SongItem): Set<Long> = buildSet {
    // 候选索引只缩小搜索范围，是否收藏仍由仓库的匹配规则决定
    add(song.identity().id)
    song.remoteDownloadIdentityOrNull()?.let { add(it.id) }
    song.matchedSongId?.toLongOrNull()?.let(::add)
}
