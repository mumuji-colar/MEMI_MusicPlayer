package com.mmc.merimusicplayer.core.player.usb.confirmation

import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.core.player.usb.path.UsbExclusiveAudioPathTracker
import com.mmc.merimusicplayer.core.player.usb.session.UsbExclusiveSessionController

internal object PlayerManagerUsbLoudPlaybackSnapshotPort {
    fun capture(): UsbExclusiveLoudPlaybackSnapshot = UsbExclusiveLoudPlaybackSnapshotSource.capture(
        UsbExclusiveLoudPlaybackSignals(
            context = PlayerManager.application,
            usbExclusiveEnabled = PlayerManager.usbExclusivePlaybackEnabled,
            appInForeground = PlayerManager.usbExclusiveAppInForeground,
            currentDevice = PlayerManager.currentAudioDeviceFlow.value,
            reportedPlaying = PlayerManager.isPlayingFlow.value,
            playerInitialized = PlayerManager::isPlayerInitialized,
            playerIsPlaying = { PlayerManager.player.isPlaying },
            playerVolume = { PlayerManager.player.volume },
            requestedVolume = { UsbExclusiveAudioPathTracker.state.value.requestedVolume },
            nativeState = { UsbExclusiveSessionController.state.value },
            bitPerfect = PlayerManager.usbExclusivePreferences.bitPerfect,
            riskThresholdDbfs = PlayerManager.usbExclusivePreferences.volumeRiskThresholdDbfs
        )
    )
}
