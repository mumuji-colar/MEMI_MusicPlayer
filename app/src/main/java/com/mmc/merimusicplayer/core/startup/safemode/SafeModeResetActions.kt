package com.mmc.merimusicplayer.core.startup.safemode

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mmc.merimusicplayer.platform.bilibili.auth.BiliCookieRepository
import com.mmc.merimusicplayer.platform.netease.auth.NeteaseCookieRepository
import com.mmc.merimusicplayer.data.auth.web.clearAllWebViewLoginState
import com.mmc.merimusicplayer.platform.youtube.auth.YouTubeAuthRepository
import com.mmc.merimusicplayer.data.model.settings.bootstrap.BootstrapSettingsSnapshot
import com.mmc.merimusicplayer.data.model.settings.playback.PlaybackPreferenceSnapshot
import com.mmc.merimusicplayer.data.model.settings.appearance.ThemePreferenceSnapshot
import com.mmc.merimusicplayer.data.settings.dataStore
import com.mmc.merimusicplayer.data.settings.bootstrap.persistBootstrapSettingsSnapshot
import com.mmc.merimusicplayer.data.settings.playback.persistPlaybackPreferenceSnapshot
import com.mmc.merimusicplayer.data.settings.appearance.persistThemePreferenceSnapshot
import com.mmc.merimusicplayer.data.settings.navigation.synchronizeStartDestinationSnapshot

internal class SafeModeResetActions(
    context: Context
) {
    private val appContext = context.applicationContext

    suspend fun clearAllCookiesAndLoginOptions() {
        withContext(Dispatchers.IO) {
            NeteaseCookieRepository(appContext).clear()
            BiliCookieRepository(appContext).clear()
            YouTubeAuthRepository(appContext).clear()
        }
        clearAllWebViewLoginState(appContext)
    }

    /** 清空设置后同步启动页缓存，安全模式下也不依赖普通观察器完成写透 */
    suspend fun resetAppSettings() {
        withContext(Dispatchers.IO) {
            synchronizeStartDestinationSnapshot(appContext) {
                appContext.dataStore.edit { prefs ->
                    prefs.clear()
                }
            }
            persistThemePreferenceSnapshot(appContext, ThemePreferenceSnapshot())
            persistBootstrapSettingsSnapshot(appContext, BootstrapSettingsSnapshot())
            persistPlaybackPreferenceSnapshot(appContext, PlaybackPreferenceSnapshot())
        }
    }
}
