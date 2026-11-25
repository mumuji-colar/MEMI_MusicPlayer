package com.mmc.merimusicplayer.ui.component.playback.lan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mmc.merimusicplayer.core.di.AppContainer
import com.mmc.merimusicplayer.data.model.lan.LanPermissions
import com.mmc.merimusicplayer.data.model.lan.LanSessionState
import com.mmc.merimusicplayer.lanltw.session.LanSessionManager

/** Keep the UI closed while joining/reconnecting, until the authoritative room arrives. */
internal fun lanPlaybackControls(state: LanSessionState): LanPermissions = when {
    !state.active -> LanPermissions()
    !state.synced || state.room == null -> LanPermissions(playPause = false, seek = false)
    state.hosting -> LanPermissions()
    else -> requireNotNull(state.room).permissions
}

internal fun lanRoomPlayPauseIntent(state: LanSessionState): String =
    LanSessionManager.playPauseIntent(state)

internal data class LanPlayerControls(val permissions: LanPermissions, val preparing: Boolean)

internal fun lanPlayerControls(state: LanSessionState) = LanPlayerControls(
    permissions = lanPlaybackControls(state),
    preparing = state.active && state.room != null && state.preparing && state.error == null
)

@Composable
internal fun rememberLanPlaybackControls(): LanPlayerControls {
    // AppContainer.initialize 之前（如纯 UI 测试）没有 LAN 会话，直接返回全允许的默认态
    if (!AppContainer.isInitialized()) return lanPlayerControls(LanSessionState())
    val state by AppContainer.lanListenTogether.state.collectAsStateWithLifecycle()
    return lanPlayerControls(state)
}
