package com.mmc.merimusicplayer.core.download.host

import android.content.Context
import com.mmc.merimusicplayer.data.model.traffic.TrafficNetworkType
import okhttp3.OkHttpClient

interface DownloadEnvironment {
    val applicationContext: Context
    val sharedOkHttpClient: OkHttpClient
    val instrumentationTestActive: Boolean

    fun recordDownloadBytes(networkType: TrafficNetworkType, bytes: Long)
    suspend fun mobileDataHighRiskPromptEnabled(): Boolean
    suspend fun awaitInteractiveContent()
    fun scheduleQuarantineRecovery(context: Context)
}
