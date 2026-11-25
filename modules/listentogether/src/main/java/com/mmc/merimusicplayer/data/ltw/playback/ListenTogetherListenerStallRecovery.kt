package com.mmc.merimusicplayer.data.ltw.playback

import com.mmc.merimusicplayer.data.ltw.mapping.ListenTogetherSongMapper
import com.mmc.merimusicplayer.data.ltw.playback.ListenTogetherPlaybackHost
import com.mmc.merimusicplayer.data.ltw.playback.currentStableKey

import androidx.media3.common.Player
import com.mmc.merimusicplayer.common.logging.NPLogger
import com.mmc.merimusicplayer.data.model.ltw.room.ListenTogetherRoomState

internal class ListenTogetherListenerStallRecovery(
    private val playback: ListenTogetherPlaybackHost,
    private val songMapper: ListenTogetherSongMapper,
    private val stallTimeoutMs: Long,
    private val recoveryCooldownMs: Long
) : ListenTogetherSongMapper by songMapper {
    private var bufferingStartedAtElapsedMs: Long = 0L
    private var bufferingTrackStableKey: String? = null
    private var lastRecoveryAtElapsedMs: Long = 0L

    fun shouldRecover(
        state: ListenTogetherRoomState,
        nowElapsedMs: Long
    ): Boolean {
        if (!isStalledTrack(state)) { resetBufferingState(); return false }
        val playerState = playback.playerPlaybackStateFlow.value
        val stableKey = state.currentStableKey() ?: run {
            resetBufferingState()
            return false
        }
        if (bufferingTrackStableKey != stableKey) {
            bufferingTrackStableKey = stableKey
            bufferingStartedAtElapsedMs = nowElapsedMs
            return false
        }
        if (nowElapsedMs - bufferingStartedAtElapsedMs < stallTimeoutMs) {
            return false
        }
        if (nowElapsedMs - lastRecoveryAtElapsedMs < recoveryCooldownMs) {
            return false
        }
        lastRecoveryAtElapsedMs = nowElapsedMs
        bufferingStartedAtElapsedMs = nowElapsedMs
        NPLogger.w(
            TAG,
            "shouldRecover(): stableKey=$stableKey, playerState=$playerState, playWhenReady=${playback.playWhenReadyFlow.value}"
        )
        return true
    }

    private fun isStalledTrack(state: ListenTogetherRoomState): Boolean {
        if (state.playback.state != "playing" || playback.isPlayingFlow.value) return false
        val target = state.targetSongItem() ?: return false
        if (playback.currentSongFlow.value?.sameTrackAs(target) != true) return false
        return looksStalled()
    }

    private fun looksStalled(): Boolean {
        val playerState = playback.playerPlaybackStateFlow.value
        return playback.playWhenReadyFlow.value || playback.isPendingMediaLoadActive() || playerState == Player.STATE_BUFFERING || playerState == Player.STATE_IDLE
    }

    fun reset() {
        lastRecoveryAtElapsedMs = 0L
        resetBufferingState()
    }

    private fun resetBufferingState() {
        bufferingStartedAtElapsedMs = 0L
        bufferingTrackStableKey = null
    }

    private companion object {
        const val TAG = "NERI-ListenTogether"
    }
}
