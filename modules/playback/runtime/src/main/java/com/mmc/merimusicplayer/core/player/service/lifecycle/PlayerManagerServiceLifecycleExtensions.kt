package com.mmc.merimusicplayer.core.player.service.lifecycle

import com.mmc.merimusicplayer.common.logging.NPLogger
import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.core.player.currentPositionMsOr
import com.mmc.merimusicplayer.core.player.persistence.scheduleStatePersist
import com.mmc.merimusicplayer.core.player.watchdog.cancelPlaybackStartupWatchdog

internal fun PlayerManager.suspendPlaybackForServiceRestart(reason: String) {
    if (!isPlayerInitialized() || currentSongFlow.value == null) return
    val shouldResume = isTransportActiveWithoutInitialization()
    val reportedPositionMs = playbackPositionFlow.value.coerceAtLeast(0L)
    val positionMs = if (player.currentMediaItem == null) reportedPositionMs
        else player.currentPositionMsOr(reportedPositionMs)

    playbackRequestToken += 1L
    playJob?.cancel()
    playJob = null
    cancelPlaybackStartupWatchdog(reason = "service_restart")
    cancelPendingPauseRequest(resetVolumeToFull = true)
    cancelVolumeFade(resetToFull = true)
    runCatching {
        player.playWhenReady = false
        player.stop()
    }.onFailure { error ->
        NPLogger.w(
            "NERI-PlayerManager",
            "failed to suspend playback for service restart reason=$reason",
            error
        )
    }

    _isPlayingFlow.value = false
    _playWhenReadyFlow.value = false
    _playbackPositionMs.value = positionMs
    stopProgressUpdates()
    setRestoredPlayback(positionMs, shouldResume)
    updateResumePlaybackRequested(shouldResume)
    scheduleStatePersist(
        positionMs = positionMs,
        shouldResumePlayback = shouldResume,
        debounceMs = 0L
    )
    NPLogger.i(
        "NERI-PlayerManager",
        "suspended playback for service restart reason=$reason positionMs=$positionMs resume=$shouldResume"
    )
}
