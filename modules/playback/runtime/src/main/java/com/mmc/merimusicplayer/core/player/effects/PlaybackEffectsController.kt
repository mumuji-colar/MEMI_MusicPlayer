package com.mmc.merimusicplayer.core.player.effects

import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.mmc.merimusicplayer.core.player.audio.processing.PlaybackVolumeBalanceState
import com.mmc.merimusicplayer.core.player.audio.processing.PlaybackVolumeNormalizationState
import com.mmc.merimusicplayer.data.model.playback.PlaybackSoundConfig
import com.mmc.merimusicplayer.data.model.playback.PlaybackSoundState
import com.mmc.merimusicplayer.data.model.playback.normalizePlaybackPitch
import com.mmc.merimusicplayer.data.model.playback.normalizePlaybackSpeed
import com.mmc.merimusicplayer.data.model.playback.normalizePlaybackVolumeBalance

/**
 * 统一管理倍速、音调、声道平衡和响度均衡, 避免这些逻辑散在 PlayerManager 里
 */
class PlaybackEffectsController {
    private var player: ExoPlayer? = null
    private var config = PlaybackSoundConfig()

    fun attachPlayer(player: ExoPlayer?): PlaybackSoundState {
        this.player = player
        applyPlaybackParameters()
        return buildState()
    }

    fun updateConfig(newConfig: PlaybackSoundConfig): PlaybackSoundState {
        val previousConfig = config
        config = newConfig.copy(
            speed = normalizePlaybackSpeed(newConfig.speed),
            pitch = normalizePlaybackPitch(newConfig.pitch),
            volumeBalance = normalizePlaybackVolumeBalance(newConfig.volumeBalance)
        )
        if (previousConfig.speed != config.speed || previousConfig.pitch != config.pitch) {
            applyPlaybackParameters()
        }
        PlaybackVolumeBalanceState.update(config.volumeBalance)
        PlaybackVolumeNormalizationState.updateEnabled(config.volumeNormalizationEnabled)
        return buildState()
    }

    fun release(): PlaybackSoundState {
        PlaybackVolumeBalanceState.update(0f)
        PlaybackVolumeNormalizationState.updateEnabled(false)
        player = null
        return buildState()
    }

    private fun applyPlaybackParameters() {
        val currentPlayer = player ?: return
        runCatching {
            currentPlayer.playbackParameters = PlaybackParameters(config.speed, config.pitch)
        }
    }

    private fun buildState(): PlaybackSoundState = PlaybackSoundState(
        speed = config.speed,
        pitch = config.pitch,
        volumeBalance = config.volumeBalance,
        volumeNormalizationEnabled = config.volumeNormalizationEnabled
    )
}
