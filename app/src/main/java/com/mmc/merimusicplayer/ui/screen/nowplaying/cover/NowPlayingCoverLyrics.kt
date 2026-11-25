package com.mmc.merimusicplayer.ui.screen.nowplaying.cover

import com.mmc.merimusicplayer.data.identity.stableKey

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.stableKey
import com.mmc.merimusicplayer.data.settings.lyrics.scaledLyricFontSize
import com.mmc.merimusicplayer.data.model.lyrics.LyricEntry
import com.mmc.merimusicplayer.lyrics.parser.flattenWordTimedEntries
import com.mmc.merimusicplayer.lyrics.parser.hasWordTimedEntries
import com.mmc.merimusicplayer.ui.component.lyrics.LyricVisualSpec
import com.mmc.merimusicplayer.ui.component.lyrics.SyncedLyricsView
import com.mmc.merimusicplayer.ui.component.lyrics.resolveLyricEdgeFadeHeight

internal data class NowPlayingSyncedLyricContent(
    val lines: List<LyricEntry>,
    val translatedLines: List<LyricEntry>?,
    val showTranslatedLines: Boolean,
    val playbackSessionKey: String?,
    val offsetMs: Long,
    val bilingualDisplayEnabled: Boolean = true
)

internal fun buildNowPlayingSyncedLyricContent(
    lines: List<LyricEntry>,
    secondaryLines: List<LyricEntry>,
    showSecondary: Boolean,
    usePhonetic: Boolean,
    song: SongItem?,
    offsetMs: Long
): NowPlayingSyncedLyricContent = NowPlayingSyncedLyricContent(
    // 展平后无法辨认逐词来源，需要同时保留普通显示的双语处理开关
    lines = lines.flattenWordTimedEntries(),
    translatedLines = nowPlayingSecondaryLyrics(showSecondary, secondaryLines),
    showTranslatedLines = showSecondary && !usePhonetic,
    playbackSessionKey = song?.stableKey(),
    offsetMs = offsetMs,
    bilingualDisplayEnabled = !lines.hasWordTimedEntries()
)

internal data class NowPlayingEmbeddedLyricStyle(
    val fontScale: Float,
    val translationFontScale: Float,
    val blurEnabled: Boolean,
    val blurAmount: Float
)

internal data class NowPlayingSyncedLyricPlayback(
    val positionFlow: StateFlow<Long>,
    val previewPositionMs: Long?,
    val isPlaying: Boolean,
    val speed: Float
)

internal data class NowPlayingSyncedLyricStyle(
    val textColor: Color,
    val fontSize: TextUnit,
    val translationFontSize: TextUnit,
    val visualSpec: LyricVisualSpec,
    val blurEnabled: Boolean,
    val blurAmount: Float
)

internal data class NowPlayingSyncedLyricActions(
    val onClick: (LyricEntry) -> Unit,
    val onLongClick: (LyricEntry) -> Unit
)

internal fun shouldShowNowPlayingEmbeddedLyrics(
    wideLandscape: Boolean,
    enabled: Boolean,
    hasLyrics: Boolean
): Boolean = !wideLandscape && enabled && hasLyrics

internal fun nowPlayingSecondaryLyrics(
    showSecondary: Boolean,
    lines: List<LyricEntry>
): List<LyricEntry>? = if (showSecondary) lines else null

internal fun shouldAdvanceNowPlayingLyrics(
    isPlaying: Boolean,
    previewPositionMs: Long?
): Boolean = isPlaying && previewPositionMs == null

internal fun resolveNowPlayingLyricsPosition(
    previewPositionMs: Long?,
    playbackPositionMs: Long
): Long = previewPositionMs ?: playbackPositionMs

@Composable
internal fun ColumnScope.NowPlayingEmbeddedLyrics(
    visible: Boolean,
    content: NowPlayingSyncedLyricContent,
    style: NowPlayingEmbeddedLyricStyle,
    playback: NowPlayingSyncedLyricPlayback,
    actions: NowPlayingSyncedLyricActions
) {
    if (!visible) return
    Spacer(Modifier.weight(1f))
    NowPlayingEmbeddedLyricsContent(
        content = content,
        style = style,
        playback = playback,
        actions = actions,
        modifier = Modifier.fillMaxWidth().weight(8f)
    )
}

@Composable
internal fun NowPlayingEmbeddedLyricsContent(
    content: NowPlayingSyncedLyricContent,
    style: NowPlayingEmbeddedLyricStyle,
    playback: NowPlayingSyncedLyricPlayback,
    actions: NowPlayingSyncedLyricActions,
    modifier: Modifier
) {
    NowPlayingLyricsPane(
        content = content,
        playback = playback.copy(
            isPlaying = shouldAdvanceNowPlayingLyrics(
                playback.isPlaying, playback.previewPositionMs
            )
        ),
        modifier = modifier,
        style = NowPlayingSyncedLyricStyle(
            textColor = MaterialTheme.colorScheme.onBackground,
            fontSize = scaledLyricFontSize(18f, style.fontScale).sp,
            translationFontSize = scaledLyricFontSize(14f, style.translationFontScale).sp,
            visualSpec = LyricVisualSpec(),
            blurEnabled = style.blurEnabled,
            blurAmount = style.blurAmount
        ),
        actions = actions
    )
}

@Composable
internal fun NowPlayingLyricsPane(
    content: NowPlayingSyncedLyricContent,
    style: NowPlayingSyncedLyricStyle,
    playback: NowPlayingSyncedLyricPlayback,
    actions: NowPlayingSyncedLyricActions,
    modifier: Modifier
) {
    val currentPosition by playback.positionFlow.collectAsStateWithLifecycle()
    SyncedLyricsView(
        lyrics = content.lines,
        bilingualDisplayEnabled = content.bilingualDisplayEnabled,
        currentTimeMs = resolveNowPlayingLyricsPosition(playback.previewPositionMs, currentPosition),
        modifier = modifier,
        textColor = style.textColor,
        fontSize = style.fontSize,
        translationFontSize = style.translationFontSize,
        visualSpec = style.visualSpec,
        lyricOffsetMs = content.offsetMs,
        lyricBlurEnabled = style.blurEnabled,
        lyricBlurAmount = style.blurAmount,
        onLyricClick = actions.onClick,
        onLyricLongClick = actions.onLongClick,
        translatedLyrics = content.translatedLines,
        isPlaying = playback.isPlaying,
        playbackSpeed = playback.speed,
        interpolatePlaybackPosition = true,
        visualEffectsEnabled = false,
        smoothActiveLineProgress = false,
        edgeFadeHeight = resolveLyricEdgeFadeHeight(isEmbedded = true),
        showEmbeddedTranslations = content.showTranslatedLines,
        playbackSessionKey = content.playbackSessionKey,
        stableEmbeddedViewport = true
    )
}
