package com.mmc.merimusicplayer.data.settings.usb

import com.mmc.merimusicplayer.data.model.settings.usb.UsbExclusivePreferences
import com.mmc.merimusicplayer.data.settings.playback.sanitized

import com.mmc.merimusicplayer.data.model.settings.playback.PlaybackPreferenceSnapshot

fun PlaybackPreferenceSnapshot.toUsbExclusivePreferences(): UsbExclusivePreferences {
    val normalizedSnapshot = sanitized()
    return UsbExclusivePreferences.fromStorageValues(
        selectedDeviceKey = normalizedSnapshot.usbExclusiveDeviceKey,
        sampleRateMode = normalizedSnapshot.usbExclusiveSampleRateMode,
        bitDepthMode = normalizedSnapshot.usbExclusiveBitDepthMode,
        bitPerfect = normalizedSnapshot.usbExclusiveBitPerfect,
        bufferProfile = normalizedSnapshot.usbExclusiveBufferProfile,
        unsupportedFormatPolicy = normalizedSnapshot.usbExclusiveUnsupportedFormatPolicy,
        sampleRateCompatibilityEnabled =
            normalizedSnapshot.usbExclusiveSampleRateCompatibility,
        bitDepthCompatibilityEnabled =
            normalizedSnapshot.usbExclusiveBitDepthCompatibility,
        channelCompatibilityEnabled =
            normalizedSnapshot.usbExclusiveChannelCompatibility,
        foregroundBufferMs = normalizedSnapshot.usbExclusiveForegroundBufferMs,
        backgroundBufferMs = normalizedSnapshot.usbExclusiveBackgroundBufferMs,
        volumeRiskThresholdDbfs = normalizedSnapshot.usbExclusiveVolumeRiskThresholdDbfs
    )
}
