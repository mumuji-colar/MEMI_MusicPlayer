package com.mmc.merimusicplayer.core.player.playback

import com.mmc.merimusicplayer.core.player.runtime.progress.PlaybackProgressPort

import com.mmc.merimusicplayer.core.player.host.PlayerDependencies
import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.core.player.currentPositionMsOrZero
import com.mmc.merimusicplayer.core.player.durationMsOrZero
import com.mmc.merimusicplayer.core.player.persistence.scheduleStatePersist
import com.mmc.merimusicplayer.data.model.SongItem

internal object PlayerManagerPlaybackProgressPort : PlaybackProgressPort {
    override fun rememberLongFormEnabled(): Boolean = PlayerManager.rememberLongFormPlaybackProgressEnabled

    override fun rememberedPosition(song: SongItem): Long =
        PlayerDependencies.repositories.playHistoryRepo.rememberedPlaybackPosition(song)

    override fun writeRememberedPosition(song: SongItem, positionMs: Long) {
        PlayerDependencies.repositories.playHistoryRepo.updateRememberedPlaybackPosition(song, positionMs)
    }

    override fun currentSong(): SongItem? = PlayerManager.currentSongFlow.value

    override fun reportedPositionMs(): Long = PlayerManager.playbackPositionFlow.value

    override fun playerPositionMs(): Long? {
        if (!PlayerManager.isPlayerInitialized()) return null
        return PlayerManager.player.currentPositionMsOrZero
    }

    override fun playerDurationMs(): Long? {
        if (!PlayerManager.isPlayerInitialized()) return null
        return PlayerManager.player.durationMsOrZero
    }

    override fun updateQueuedDurationIfUnknown(song: SongItem, durationMs: Long): Boolean =
        PlayerManager.updateQueuedSong(song) { queued ->
            queuedDurationIfUnknown(queued, durationMs)
        } != null

    private fun queuedDurationIfUnknown(queued: SongItem, durationMs: Long): SongItem? =
        if (queued.durationMs <= 0L) queued.copy(durationMs = durationMs) else null

    override fun replaceCurrentSong(song: SongItem) {
        PlayerManager.setCurrentSongForPlayback(song)
    }

    override fun scheduleImmediateStatePersist() {
        PlayerManager.scheduleStatePersist(debounceMs = 0L)
    }

    override fun pendingMediaLoadActive(): Boolean = PlayerManager.isPendingMediaLoadActive()

    override fun pendingMediaLoadPositionMs(): Long = PlayerManager.pendingMediaLoadPositionMs
}
