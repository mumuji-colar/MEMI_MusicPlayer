package com.mmc.merimusicplayer.core.player.audio.output

import com.mmc.merimusicplayer.core.player.host.settingsRepo
import com.mmc.merimusicplayer.lyrics.lyricon.LyriconManager
import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.core.player.lifecycle.scheduleUsbAudioSinkReconfiguration
import com.mmc.merimusicplayer.core.player.lifecycle.updateAudioOffloadPreferences
import com.mmc.merimusicplayer.data.model.playback.PlaybackSoundConfig

internal object PlayerManagerPlaybackSoundPort : PlaybackSoundPort {
    override fun lyriconEnabled(): Boolean = PlayerManager.lyriconEnabled

    override fun usbExclusiveEnabled(): Boolean = PlayerManager.usbExclusivePlaybackEnabled

    override fun updateLyriconSpeed(speed: Float) {
        LyriconManager.setPlaybackSpeed(speed)
    }

    override fun updateAudioOffloadPreferences(reason: String) {
        PlayerManager.updateAudioOffloadPreferences(reason)
    }

    override fun reconfigureUsbSinkForHighResolution() {
        PlayerManager.scheduleUsbAudioSinkReconfiguration(
            reason = "playback_high_resolution_output_changed",
            allowWhilePlaybackActive = true,
            bypassCooldown = true
        )
    }

    override fun debugStackHint(): String = PlayerManager.debugStackHint()

    override suspend fun persistConfig(config: PlaybackSoundConfig) {
        settingsRepo.setPlaybackSpeed(config.speed)
        settingsRepo.setPlaybackPitch(config.pitch)
        settingsRepo.setPlaybackVolumeBalance(config.volumeBalance)
        settingsRepo.setPlaybackVolumeNormalizationEnabled(config.volumeNormalizationEnabled)
    }

    override suspend fun persistHighResolution(enabled: Boolean) {
        settingsRepo.setPlaybackHighResolutionOutputEnabled(enabled)
    }
}
