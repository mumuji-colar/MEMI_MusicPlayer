package com.mmc.merimusicplayer.data.ltw.platform

import android.content.Context
import com.mmc.merimusicplayer.data.model.ltw.session.ListenTogetherValidationError

interface ListenTogetherPlatformHost {
    val applicationContext: Context
    val networkMonitor: ListenTogetherNetworkMonitor
    fun isInitialized(): Boolean
    fun isPlaybackServiceReady(): Boolean
    fun startForegroundSync(reason: String)
    fun message(resourceId: Int): String
    fun validationMessage(error: ListenTogetherValidationError): String
}
