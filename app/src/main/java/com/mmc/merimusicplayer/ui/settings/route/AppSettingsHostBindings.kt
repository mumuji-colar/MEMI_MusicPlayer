package com.mmc.merimusicplayer.ui.settings.route

import androidx.compose.ui.geometry.Offset
import com.mmc.merimusicplayer.data.listentogether.ListenTogetherPreferences
import com.mmc.merimusicplayer.data.settings.SettingsRepository
import com.mmc.merimusicplayer.data.model.settings.appearance.ThemeMode
import com.mmc.merimusicplayer.data.ltw.ListenTogetherSessionManager
import com.mmc.merimusicplayer.api.ltw.http.ListenTogetherApi
import com.mmc.merimusicplayer.ui.settings.owner.AppUsbExclusiveSettingsActions
import com.mmc.merimusicplayer.common.locale.LanguageManager
import com.mmc.merimusicplayer.navigation.Destinations

internal fun isAppSettingsVisible(selectedMainTabRoute: String, playbackOpen: Boolean): Boolean =
    selectedMainTabRoute == Destinations.Settings.route && !playbackOpen

internal class AppSettingsHostEnvironment(
    val isDarkTheme: Boolean,
    val themeMode: ThemeMode,
    val onThemeToggleRequest: (Offset, Float) -> Unit,
    val onThemeModeRequest: (ThemeMode, Offset, Float) -> Unit,
    val backgroundImageAlpha: Float,
    val defaultStartDestination: String,
    val homeHasRecentUsage: Boolean,
    val onBeforeLanguageRestart: () -> Unit,
    val onLanguageChanged: (LanguageManager.Language) -> Unit,
    val coherentFeedbackEnabled: Boolean,
    val settingsVisible: Boolean = true
)

internal class AppSettingsHostBindings(
    val repository: SettingsRepository,
    val listenTogether: AppSettingsListenTogetherDependencies,
    val state: AppSettingsRouteState,
    val appearanceActions: AppAppearanceSettingsActions,
    val lyricsActions: AppLyricSettingsActions,
    val qualityActions: AppAudioQualitySettingsActions,
    val playbackActions: AppPlaybackSettingsActions,
    val usbActions: AppUsbExclusiveSettingsActions,
    val homeActions: AppHomeSettingsActions,
    val storageActions: AppStorageSettingsActions,
    val environment: AppSettingsHostEnvironment
)

internal class AppSettingsListenTogetherDependencies(
    val preferences: ListenTogetherPreferences,
    val api: ListenTogetherApi,
    val sessionManager: ListenTogetherSessionManager
)
