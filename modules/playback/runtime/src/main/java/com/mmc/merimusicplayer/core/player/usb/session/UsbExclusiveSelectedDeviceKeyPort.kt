package com.mmc.merimusicplayer.core.player.usb.session

import android.content.Context
import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.data.settings.playback.readPlaybackPreferenceSnapshotSync
import com.mmc.merimusicplayer.data.settings.usb.toUsbExclusivePreferences

internal fun interface UsbExclusiveSelectedDeviceKeyPort {
    fun readSelectedDeviceKey(context: Context): String
}

internal object AndroidUsbExclusiveSelectedDeviceKeyPort : UsbExclusiveSelectedDeviceKeyPort {
    override fun readSelectedDeviceKey(context: Context): String {
        if (PlayerManager.isPlayerInitialized()) {
            return PlayerManager.usbExclusivePreferences.selectedDeviceKey
        }
        return readPlaybackPreferenceSnapshotSync(context)
            .toUsbExclusivePreferences()
            .selectedDeviceKey
    }
}
