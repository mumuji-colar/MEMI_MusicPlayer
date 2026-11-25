package com.mmc.merimusicplayer.ui.settings.owner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mmc.merimusicplayer.data.settings.SettingsRepository
import com.mmc.merimusicplayer.data.model.settings.usb.UsbExclusiveBitDepthMode
import com.mmc.merimusicplayer.data.model.settings.usb.UsbExclusiveBufferProfile
import com.mmc.merimusicplayer.data.model.settings.usb.UsbExclusiveSampleRateMode
import com.mmc.merimusicplayer.data.model.settings.usb.UsbExclusiveUnsupportedFormatPolicy

internal class AppUsbExclusiveSettingsActions(
    private val repo: SettingsRepository,
    private val scope: CoroutineScope
) {
    val onDeviceKeyChange: (String) -> Unit = { value ->
        scope.launch { repo.setUsbExclusiveDeviceKey(value) }
    }
    val onSampleRateModeChange: (UsbExclusiveSampleRateMode) -> Unit = { value ->
        scope.launch { repo.setUsbExclusiveSampleRateMode(value) }
    }
    val onBitDepthModeChange: (UsbExclusiveBitDepthMode) -> Unit = { value ->
        scope.launch { repo.setUsbExclusiveBitDepthMode(value) }
    }
    val onBitPerfectChange: (Boolean) -> Unit = { value ->
        scope.launch { repo.setUsbExclusiveBitPerfect(value) }
    }
    val onBufferProfileChange: (UsbExclusiveBufferProfile) -> Unit = { value ->
        scope.launch { repo.setUsbExclusiveBufferProfile(value) }
    }
    val onUnsupportedFormatPolicyChange: (UsbExclusiveUnsupportedFormatPolicy) -> Unit = { value ->
        scope.launch { repo.setUsbExclusiveUnsupportedFormatPolicy(value) }
    }
    val onSampleRateCompatibilityChange: (Boolean) -> Unit = { value ->
        scope.launch { repo.setUsbExclusiveSampleRateCompatibility(value) }
    }
    val onBitDepthCompatibilityChange: (Boolean) -> Unit = { value ->
        scope.launch { repo.setUsbExclusiveBitDepthCompatibility(value) }
    }
    val onChannelCompatibilityChange: (Boolean) -> Unit = { value ->
        scope.launch { repo.setUsbExclusiveChannelCompatibility(value) }
    }
    val onForegroundBufferMsChange: (Int) -> Unit = { value ->
        scope.launch { repo.setUsbExclusiveForegroundBufferMs(value) }
    }
    val onBackgroundBufferMsChange: (Int) -> Unit = { value ->
        scope.launch { repo.setUsbExclusiveBackgroundBufferMs(value) }
    }
    val onVolumeRiskThresholdDbfsChange: (Int) -> Unit = { value ->
        scope.launch { repo.setUsbExclusiveVolumeRiskThresholdDbfs(value) }
    }
}
