package com.mmc.merimusicplayer.core.player.persistence

import com.mmc.merimusicplayer.data.identity.sameIdentityAs

import com.mmc.merimusicplayer.data.model.playback.PersistedPlaybackState
import com.mmc.merimusicplayer.data.model.playback.PersistedState
import com.mmc.merimusicplayer.data.model.playback.queue.PlayerQueueSnapshot
import com.mmc.merimusicplayer.data.model.playback.queue.PlayerQueueSessionSnapshot
import com.mmc.merimusicplayer.core.player.persistence.toPersistedSongItem
import com.mmc.merimusicplayer.core.player.persistence.withPlaybackState
import com.mmc.merimusicplayer.data.model.SongItem

internal enum class PlaybackStateWrite {
    NONE,
    CLEAR,
    REPLACE_QUEUE,
    UPDATE_PLAYBACK
}

internal class PlaybackStatePersistenceSnapshot(
    val queue: PlayerQueueSnapshot,
    val playback: PersistedPlaybackState,
    shuffleRestorePlaylist: List<SongItem>?,
    shuffleRestoreIndex: Int
) {
    constructor(
        session: PlayerQueueSessionSnapshot,
        playback: PersistedPlaybackState,
        keepShuffleMode: Boolean
    ) : this(
        session.queue,
        playback.copy(shuffleEnabled = keepShuffleMode && session.shuffleEnabled),
        session.shuffleRestore?.playlist,
        session.shuffleRestore?.currentIndex ?: -1
    )

    private val shuffleRestorePlaylist = enabledRestorePlaylist(shuffleRestorePlaylist)
    private val shuffleRestoreIndex = resolveRestoreIndex(shuffleRestoreIndex)

    init {
        require(playback.index == queue.currentIndex)
        require(playback.positionMs >= 0L)
    }

    fun writeAfter(previous: PlaybackStatePersistenceSnapshot?): PlaybackStateWrite {
        if (queue.playlist.isEmpty()) return emptyQueueWrite(previous)
        if (requiresQueueReplacement(previous)) return PlaybackStateWrite.REPLACE_QUEUE
        return if (playback != previous?.playback) {
            PlaybackStateWrite.UPDATE_PLAYBACK
        } else {
            PlaybackStateWrite.NONE
        }
    }

    private fun enabledRestorePlaylist(playlist: List<SongItem>?): List<SongItem>? =
        if (playback.shuffleEnabled == true) playlist?.toList() else null

    private fun resolveRestoreIndex(fallback: Int): Int? {
        if (playback.shuffleEnabled != true) return null
        return currentSongRestoreIndex() ?: fallback.takeIf { it >= 0 }
    }

    private fun currentSongRestoreIndex(): Int? {
        val currentSong = queue.playlist.getOrNull(queue.currentIndex) ?: return null
        return shuffleRestorePlaylist?.indexOfFirst { currentSong.sameIdentityAs(it) }?.takeIf { it >= 0 }
    }

    private fun emptyQueueWrite(previous: PlaybackStatePersistenceSnapshot?): PlaybackStateWrite =
        if (previous?.queue?.playlist?.isEmpty() == true && playback == previous.playback) {
            PlaybackStateWrite.NONE
        } else {
            PlaybackStateWrite.CLEAR
        }

    private fun requiresQueueReplacement(previous: PlaybackStatePersistenceSnapshot?): Boolean =
        previous == null || queue.playlist !== previous.queue.playlist ||
            shuffleRestorePlaylist != previous.shuffleRestorePlaylist ||
            shuffleRestoreIndex != previous.shuffleRestoreIndex

    fun toPersistedState(): PersistedState {
        val currentSong = queue.playlist.getOrNull(queue.currentIndex)
        return PersistedState(
            playlist = queue.playlist.mapIndexed { index, song ->
                // 仅保存当前歌曲的内嵌歌词，避免反复序列化整队长文本
                song.toPersistedSongItem(includeLyrics = index == queue.currentIndex)
            },
            index = queue.currentIndex,
            shuffleRestorePlaylist = shuffleRestorePlaylist?.map { song ->
                song.toPersistedSongItem(includeLyrics = currentSong?.sameIdentityAs(song) == true)
            },
            shuffleRestoreIndex = shuffleRestoreIndex
        ).withPlaybackState(playback)
    }
}
