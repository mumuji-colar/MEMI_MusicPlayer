package com.mmc.merimusicplayer.core.player.policy.command

import com.mmc.merimusicplayer.data.model.playback.PlaybackSoundConfig
import com.mmc.merimusicplayer.data.model.playback.normalizePlaybackPitch
import com.mmc.merimusicplayer.data.model.playback.normalizePlaybackSpeed
import com.mmc.merimusicplayer.data.model.playback.normalizePlaybackVolumeBalance

fun resolvePlaybackSoundConfigForEngine(
    baseConfig: PlaybackSoundConfig,
    listenTogetherSyncPlaybackRate: Float,
    usbExclusivePlaybackEnabled: Boolean = false
): PlaybackSoundConfig {
    val normalizedBaseConfig = baseConfig.copy(
            speed = normalizePlaybackSpeed(baseConfig.speed),
            pitch = normalizePlaybackPitch(baseConfig.pitch),
            volumeBalance = normalizePlaybackVolumeBalance(baseConfig.volumeBalance)
        )
    if (usbExclusivePlaybackEnabled) {
        return normalizedBaseConfig.copy(
            speed = 1f,
            pitch = 1f,
            volumeBalance = 0f,
            volumeNormalizationEnabled = false
        )
    }
    val resolvedSyncRate = listenTogetherSyncPlaybackRate.coerceIn(0.95f, 1.05f)
    return normalizedBaseConfig.copy(
        speed = normalizePlaybackSpeed(normalizedBaseConfig.speed * resolvedSyncRate)
    )
}
