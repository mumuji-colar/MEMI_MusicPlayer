@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.mmc.merimusicplayer.core.player.lifecycle

import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.core.player.session.AppQueueSongIdentity
import com.mmc.merimusicplayer.data.model.playback.PlayerEvent
import com.mmc.merimusicplayer.platform.subsonic.api.SubsonicException
import com.mmc.merimusicplayer.platform.subsonic.api.subsonicErrorMessageRes

/** Preserve the current server track after transport retries so manual retry can resume it. */
internal fun PlayerManager.handleServerPlaybackError(error: Throwable): Boolean {
    val serverError = SubsonicException.find(error) ?: return false
    val failedSong = _currentSongFlow.value
    val failedPosition = player.currentPosition.coerceAtLeast(0L)
    pause()
    if (failedSong != null) {
        serverRecoveryPosition = AppQueueSongIdentity.stableKey(failedSong) to failedPosition
    }
    postPlayerEvent(PlayerEvent.ShowError(getLocalizedString(subsonicErrorMessageRes(serverError))))
    return true
}
