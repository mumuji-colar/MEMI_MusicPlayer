package com.mmc.merimusicplayer.core.startup.player

import android.app.Application
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.core.player.persistence.RestoredPlayerStatePreload
import com.mmc.merimusicplayer.core.player.persistence.initializePreloadedRestoration
import com.mmc.merimusicplayer.core.player.persistence.preloadRestoredPlayerState
import com.mmc.merimusicplayer.data.model.settings.playback.PlaybackPreferenceSnapshot
import com.mmc.merimusicplayer.data.settings.playback.readPlaybackPreferenceSnapshot

/**
 * 播放偏好和上次播放快照都在 IO 上预读，主线程只做内存态初始化；
 * 裸 PlayerManager.initialize() 会在调用线程同步读取设置与 Room，主线程调用会造成 ANR
 */
internal class PlayerPreloadedInitializer(
    private val app: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val readPreferences: suspend (Application) -> PlaybackPreferenceSnapshot = { application ->
        readPlaybackPreferenceSnapshot(application)
    },
    private val preloadState: suspend (Application, PlaybackPreferenceSnapshot) -> RestoredPlayerStatePreload =
        { application, preferences ->
            preloadRestoredPlayerState(
                app = application,
                keepLastPlaybackProgressEnabled = preferences.keepLastPlaybackProgress,
                keepPlaybackModeStateEnabled = preferences.keepPlaybackModeState
            )
        },
    private val initializePlayer: (Application, PlaybackPreferenceSnapshot, RestoredPlayerStatePreload) -> Unit =
        { application, preferences, restoredState ->
            PlayerManager.initializePreloadedRestoration(
                app = application,
                startupPlaybackPreferences = preferences,
                restorationPreload = restoredState
            )
        }
) {
    suspend fun initialize(beforeInitialize: suspend () -> Unit = {}): PlaybackPreferenceSnapshot {
        val preferences = withContext(ioDispatcher) { readPreferences(app) }
        val restoredState = withContext(ioDispatcher) { preloadState(app, preferences) }
        beforeInitialize()
        initializePlayer(app, preferences, restoredState)
        return preferences
    }
}
