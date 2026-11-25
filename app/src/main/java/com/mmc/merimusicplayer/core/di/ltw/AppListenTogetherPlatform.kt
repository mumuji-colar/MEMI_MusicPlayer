package com.mmc.merimusicplayer.core.di.ltw

import com.mmc.merimusicplayer.core.di.AppContainer
import com.mmc.merimusicplayer.core.player.service.AudioPlayerService
import com.mmc.merimusicplayer.data.ltw.platform.ListenTogetherPlatformHost
import com.mmc.merimusicplayer.data.ltw.validation.format
import com.mmc.merimusicplayer.data.model.ltw.session.ListenTogetherValidationError

internal object AppListenTogetherPlatform : ListenTogetherPlatformHost {
    override val applicationContext get() = AppContainer.applicationContext
    override val networkMonitor = AndroidListenTogetherNetworkMonitor { applicationContext }
    override fun isInitialized(): Boolean = AppContainer.isInitialized()
    override fun isPlaybackServiceReady(): Boolean = AudioPlayerService.isReadyForPassiveLocalPlaybackSync()
    override fun startForegroundSync(reason: String) {
        AudioPlayerService.startSyncService(applicationContext, "listen_together_$reason", forceForeground = true)
    }
    override fun message(resourceId: Int): String = applicationContext.getString(resourceId)
    override fun validationMessage(error: ListenTogetherValidationError): String = error.format(applicationContext)
}
