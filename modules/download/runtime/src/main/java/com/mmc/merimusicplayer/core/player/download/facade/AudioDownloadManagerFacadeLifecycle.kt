package com.mmc.merimusicplayer.core.player.download.facade

import com.mmc.merimusicplayer.core.download.host.DownloadHosts
import com.mmc.merimusicplayer.core.player.download.AudioDownloadManager
import com.mmc.merimusicplayer.core.player.download.network.handleDefaultDownloadNetworkCallback
import com.mmc.merimusicplayer.core.player.download.network.handleDefaultDownloadNetworkLost
import com.mmc.merimusicplayer.core.player.download.network.shouldNotifyRecoveryForConfirmedInternet
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.mmc.merimusicplayer.data.traffic.downloadNetworkTypeOrNull

internal fun AudioDownloadManager.initializeImpl(context: Context) {
    val appContext = context.applicationContext
    synchronized(networkRecoveryMonitorLock) {
        if (networkRecoveryMonitorRegistered) {
            return
        }
        val connectivityManager: ConnectivityManager =
            appContext.getSystemService(ConnectivityManager::class.java) ?: return
        val initialNetwork = connectivityManager.activeNetwork
        val initialNetworkType = initialNetwork
            ?.let { network -> connectivityManager.getNetworkCapabilities(network) }
            ?.downloadNetworkTypeOrNull()
        val persistedNetworkGeneration = appContext
            .getSharedPreferences(DOWNLOAD_NETWORK_POLICY_PREFS, Context.MODE_PRIVATE)
            .getLong(NETWORK_GENERATION_PREF, 0L)
        downloadNetworkPolicyTracker.seed(
            networkKey = initialNetwork,
            networkType = initialNetworkType,
            initialGeneration = persistedNetworkGeneration
        )
        // 仪器测试会切换共享 SAF 夹具，系统网络回调不能在测试中异步恢复这些文件
        if (DownloadHosts.environment.instrumentationTestActive) return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handleDefaultDownloadNetworkCallback(
                    context = appContext,
                    connectivityManager = connectivityManager,
                    callbackNetwork = network,
                    reason = "network_available"
                )
                if (shouldNotifyRecoveryForConfirmedInternet(appContext)) {
                    notifyRecoveryOpportunity("network_available")
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                _networkCapabilities: NetworkCapabilities
            ) {
                handleDefaultDownloadNetworkCallback(
                    context = appContext,
                    connectivityManager = connectivityManager,
                    callbackNetwork = network,
                    reason = "network_capabilities_changed"
                )
                if (shouldNotifyRecoveryForConfirmedInternet(appContext)) {
                    notifyRecoveryOpportunity("network_available")
                }
            }

            override fun onLost(network: Network) {
                synchronized(networkRecoveryMonitorLock) {
                    lastConfirmedInternetAccess = false
                }
                handleDefaultDownloadNetworkLost(
                    context = appContext,
                    connectivityManager = connectivityManager,
                    network = network
                )
            }
        }
        val registered = runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)
            true
        }.getOrDefault(false)
        if (registered) {
            networkRecoveryMonitorRegistered = true
        }
    }
}
