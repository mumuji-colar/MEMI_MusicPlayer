package com.mmc.merimusicplayer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

typealias MemiMiniPlayerDefaults = com.mmc.merimusicplayer.ui.component.playback.MemiMiniPlayerDefaults
typealias PlaybackSourceType = com.mmc.merimusicplayer.ui.component.playback.PlaybackSourceType

@Composable
fun MemiMiniPlayer(
    title: String,
    artist: String,
    coverUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    playPauseEnabled: Boolean = true,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    enableBlur: Boolean = true,
    offlineMode: Boolean = false,
    isPlaybackWaiting: Boolean = false,
    isAudioRouteMuted: Boolean = false,
    visualCoverUrl: String? = null,
    coverIdentityKey: String? = null,
    visualCoverIdentityKey: String? = null,
    hasCurrentSong: Boolean = true,
    tabletControls: com.mmc.merimusicplayer.ui.component.playback.MiniPlayerTabletControls? = null,
    clearCover: Boolean = false
) {
    com.mmc.merimusicplayer.ui.component.playback.MemiMiniPlayer(
        title = title,
        artist = artist,
        coverUrl = coverUrl,
        isPlaying = isPlaying,
        modifier = modifier,
        playPauseEnabled = playPauseEnabled,
        onPlayPause = onPlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        onExpand = onExpand,
        enableBlur = enableBlur,
        offlineMode = offlineMode,
        isPlaybackWaiting = isPlaybackWaiting,
        isAudioRouteMuted = isAudioRouteMuted,
        visualCoverUrl = visualCoverUrl,
        coverIdentityKey = coverIdentityKey,
        visualCoverIdentityKey = visualCoverIdentityKey,
        hasCurrentSong = hasCurrentSong,
        tabletControls = tabletControls,
        clearCover = clearCover
    )
}

@Composable
fun PlaybackSourceBadge(
    source: PlaybackSourceType,
    modifier: Modifier = Modifier
) {
    com.mmc.merimusicplayer.ui.component.playback.PlaybackSourceBadge(
        source = source,
        modifier = modifier
    )
}

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit
) {
    com.mmc.merimusicplayer.ui.component.playback.SleepTimerDialog(onDismiss = onDismiss)
}

@Composable
fun WaveformSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onValueChangeStarted: (Float) -> Unit = {},
    onValueChangeCanceled: () -> Unit = {},
    enabled: Boolean = true,
    isPlaybackWaiting: Boolean = false
) {
    com.mmc.merimusicplayer.ui.component.playback.WaveformSlider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        isPlaying = isPlaying,
        onValueChangeStarted = onValueChangeStarted,
        onValueChangeCanceled = onValueChangeCanceled,
        enabled = enabled,
        modifier = modifier,
        isPlaybackWaiting = isPlaybackWaiting
    )
}
