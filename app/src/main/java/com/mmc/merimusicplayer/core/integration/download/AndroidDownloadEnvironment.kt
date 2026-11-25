package com.mmc.merimusicplayer.core.integration.download

import android.content.Context
import kotlinx.coroutines.flow.first
import com.mmc.merimusicplayer.core.di.AppContainer
import com.mmc.merimusicplayer.core.download.host.DownloadEnvironment
import com.mmc.merimusicplayer.core.startup.AppStartupWorkGate
import com.mmc.merimusicplayer.core.startup.LegacyJsonCleanupScheduler
import com.mmc.merimusicplayer.core.startup.app.InstrumentationTestRuntime
import com.mmc.merimusicplayer.data.model.traffic.TrafficNetworkType
import com.mmc.merimusicplayer.data.model.traffic.TrafficUsageSource

internal object AndroidDownloadEnvironment : DownloadEnvironment {
    override val applicationContext get() = AppContainer.applicationContext
    override val sharedOkHttpClient get() = AppContainer.sharedOkHttpClient
    override val instrumentationTestActive get() = InstrumentationTestRuntime.isActive

    override fun recordDownloadBytes(networkType: TrafficNetworkType, bytes: Long) {
        AppContainer.trafficStatsRepo.recordNetworkBytes(
            networkType = networkType,
            bytes = bytes,
            source = TrafficUsageSource.DOWNLOAD
        )
    }

    override suspend fun mobileDataHighRiskPromptEnabled(): Boolean =
        AppContainer.settingsRepo.mobileDataHighRiskPromptEnabledFlow.first()

    override suspend fun awaitInteractiveContent() {
        AppStartupWorkGate.awaitInteractiveContentOrTimeout()
    }

    override fun scheduleQuarantineRecovery(context: Context) {
        LegacyJsonCleanupScheduler.scheduleQuarantineRecovery(context)
    }
}
