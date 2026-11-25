package com.mmc.merimusicplayer.core.player.url

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.core.player.host.PlayerDependencies
import com.mmc.merimusicplayer.core.player.runtime.refresh.RefreshResolverSideEffects
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.playback.PlayerEvent
import com.mmc.merimusicplayer.data.model.playback.SongUrlResult
import com.mmc.merimusicplayer.data.model.server.ServerSongRef
import com.mmc.merimusicplayer.platform.subsonic.api.SubsonicException
import com.mmc.merimusicplayer.platform.subsonic.api.subsonicErrorMessageRes

internal suspend fun PlayerManager.hasUsableServerProfile(
    song: SongItem,
    sideEffects: RefreshResolverSideEffects
): Boolean {
    val accounts = PlayerDependencies.repositories.subsonicRepository?.accounts
    val profile = try {
        accounts?.load()
        ServerSongRef.from(song)?.let { accounts?.profile(it.profileId) }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
    if (profile != null) return true
    sideEffects.emitError {
        postPlayerEvent(PlayerEvent.ShowError(getLocalizedString(CoreCommonR.string.server_unavailable)))
    }
    return false
}

// resolve authenticated resources only after checking the current local profile
internal suspend fun PlayerManager.resolveServerSongUrl(
    song: SongItem,
    forceRefresh: Boolean,
    sideEffects: RefreshResolverSideEffects
): SongUrlResult = try {
    val result = PlayerDependencies.repositories.subsonicRepository
        ?.playback(song, forceRefresh) ?: SongUrlResult.Failure
    if (result is SongUrlResult.Success) {
        result.copy(audioInfo = result.audioInfo?.copy(
            qualityLabel = getLocalizedString(CoreCommonR.string.server_quality_original)))
    } else {
        sideEffects.emitError { postPlayerEvent(PlayerEvent.ShowError(getLocalizedString(CoreCommonR.string.server_unavailable))) }
        SongUrlResult.Failure
    }
} catch (_: TimeoutCancellationException) {
    sideEffects.emitError { postPlayerEvent(PlayerEvent.ShowError(getLocalizedString(CoreCommonR.string.server_timeout))) }
    SongUrlResult.Failure
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: SubsonicException) {
    sideEffects.emitError { postPlayerEvent(PlayerEvent.ShowError(getLocalizedString(subsonicErrorMessageRes(error)))) }
    SongUrlResult.Failure
} catch (_: Exception) {
    sideEffects.emitError { postPlayerEvent(PlayerEvent.ShowError(getLocalizedString(CoreCommonR.string.server_request_failed))) }
    SongUrlResult.Failure
}
