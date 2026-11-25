package com.mmc.merimusicplayer.core.player.audio.processing

import com.mmc.merimusicplayer.data.model.playback.DEFAULT_PLAYBACK_VOLUME_BALANCE
import com.mmc.merimusicplayer.data.model.playback.normalizePlaybackVolumeBalance

object PlaybackVolumeBalanceState {
    @Volatile
    private var balance = DEFAULT_PLAYBACK_VOLUME_BALANCE

    fun update(balance: Float) {
        this.balance = normalizePlaybackVolumeBalance(balance)
    }

    fun current(): Float = balance
}
