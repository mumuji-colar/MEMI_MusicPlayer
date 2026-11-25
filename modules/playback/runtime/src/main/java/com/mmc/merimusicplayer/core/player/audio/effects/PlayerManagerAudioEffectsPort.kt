package com.mmc.merimusicplayer.core.player.audio.effects

import com.mmc.merimusicplayer.common.logging.NPLogger
import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.core.player.host.settingsRepo
import com.mmc.merimusicplayer.core.player.lifecycle.updateAudioOffloadPreferences
import com.mmc.merimusicplayer.data.model.playback.effects.AudioEffectsSettings

internal object PlayerManagerAudioEffectsPort : AudioEffectsPort {
    override fun usbBitPerfect(): Boolean = PlayerManager.usbExclusivePreferences.bitPerfect

    override fun onDspActiveChanged(active: Boolean, reason: String) {
        NPLogger.i("NERI-AudioEffects", "DSP active=$active reason=$reason")
        PlayerManager.updateAudioOffloadPreferences("audio_effects_$reason")
    }

    override suspend fun persist(settings: AudioEffectsSettings) {
        settingsRepo.setAudioEffectsSettings(settings)
    }
}
