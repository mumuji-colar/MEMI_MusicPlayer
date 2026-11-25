package com.mmc.merimusicplayer.ui.playback.visual

import com.mmc.merimusicplayer.ui.navigation.LocalPredictiveBackPreviewEnabled
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.flow.StateFlow
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.settings.appearance.UiStyle
import com.mmc.merimusicplayer.ui.component.common.PredictiveDismissHandler
import com.mmc.merimusicplayer.ui.component.common.PredictiveDismissState
import com.mmc.merimusicplayer.ui.component.common.blockUnderlyingTouches
import com.mmc.merimusicplayer.ui.component.common.predictiveDismissTransform
import com.mmc.merimusicplayer.ui.component.common.rememberPredictiveDismissState
import com.mmc.merimusicplayer.ui.navigation.LocalMiniPlayerHeight
import com.mmc.merimusicplayer.ui.animation.LocalSharedCoverFlight
import com.mmc.merimusicplayer.ui.animation.SharedCoverFlightState
import com.mmc.merimusicplayer.util.media.RetainedPlaybackCoverBitmapCache
import com.mmc.merimusicplayer.ui.theme.MemiTheme
import com.mmc.merimusicplayer.ui.util.hasKnownEmptyLocalCover
import com.mmc.merimusicplayer.util.platform.PHONE_SMALLEST_SCREEN_WIDTH_DP

internal data class NowPlayingOverlayCover(
    val url: String?,
    val songKey: String?,
    val song: SongItem?,
    val assetRefreshKey: Int,
    val assetRefresh: NowPlayingCoverAssetRefresh? = null
) {
    val clearCover: Boolean get() = song.hasKnownEmptyLocalCover()
}

internal data class NowPlayingOverlayTheme(
    val dynamicColorEnabled: Boolean,
    val activeCoverSeedHex: String?,
    val seedColorHex: String,
    val paletteStyle: PaletteStyle,
    val colorSpec: ColorSpec.SpecVersion,
    val uiStyle: UiStyle = UiStyle.Material,
    val liquidGlass: Boolean = false
)

internal data class NowPlayingThemeSelection(
    val seedColorHex: String,
    val useSystemDynamic: Boolean
)

internal fun selectNowPlayingOverlayTheme(
    theme: NowPlayingOverlayTheme,
    coverUrl: String?
): NowPlayingThemeSelection = NowPlayingThemeSelection(
    seedColorHex = if (theme.dynamicColorEnabled) {
        theme.activeCoverSeedHex ?: theme.seedColorHex
    } else {
        theme.seedColorHex
    },
    useSystemDynamic = theme.dynamicColorEnabled &&
        theme.activeCoverSeedHex == null && coverUrl == null
)

internal data class NowPlayingOverlayBackground(
    val blurEnabled: Boolean,
    val blurAmount: Float,
    val blurDarken: Float,
    val dynamicEnabled: Boolean,
    val offlineMode: Boolean
)

internal fun releaseNowPlayingInputSession(
    focusManager: FocusManager,
    textToolbar: TextToolbar,
    keyboardController: SoftwareKeyboardController?
) {
    // 先结束底层输入会话，再关闭独立的系统选择菜单和键盘
    focusManager.clearFocus(force = true)
    textToolbar.hide()
    keyboardController?.hide()
}

internal fun resolveNowPlayingOverlayArtwork(
    songKey: String?,
    flight: SharedCoverFlightState?,
    clearCover: Boolean = false
): ImageBitmap? {
    if (clearCover) {
        flight?.clearArtwork()
        return null
    }
    return RetainedPlaybackCoverBitmapCache.getLatestForOwner(songKey)?.bitmap
        ?: flight?.miniCoverBitmap
}

internal fun shouldUseNowPlayingContainerMorph(
    smallestScreenWidthDp: Int
): Boolean = smallestScreenWidthDp < PHONE_SMALLEST_SCREEN_WIDTH_DP

@Composable
internal fun AppNowPlayingOverlay(
    visible: Boolean,
    cover: NowPlayingOverlayCover,
    queueFlow: StateFlow<List<SongItem>>,
    theme: NowPlayingOverlayTheme,
    background: NowPlayingOverlayBackground,
    onVisibilityChanged: (Boolean) -> Unit,
    onClose: () -> Unit,
    dismissState: PredictiveDismissState = rememberPredictiveDismissState(),
    content: @Composable () -> Unit
) {
    val latestOnVisibilityChanged by rememberUpdatedState(onVisibilityChanged)
    val focusManager = LocalFocusManager.current
    val textToolbar = LocalTextToolbar.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(visible) {
        if (visible) {
            dismissState.reset()
            releaseNowPlayingInputSession(focusManager, textToolbar, keyboardController)
        }
    }
    val useContainerMorph = shouldUseNowPlayingContainerMorph(
        smallestScreenWidthDp = LocalConfiguration.current.smallestScreenWidthDp
    )
    val sharedCoverFlight = LocalSharedCoverFlight.current
    val overlayBody: @Composable () -> Unit = {
        DisposableEffect(Unit) {
            latestOnVisibilityChanged(true)
            onDispose {
                latestOnVisibilityChanged(false)
            }
        }
        val themeSelection = selectNowPlayingOverlayTheme(theme, cover.url)
        MemiTheme(
            followSystemDark = false,
            forceDark = true,
            dynamicColor = themeSelection.useSystemDynamic,
            seedColorHex = themeSelection.seedColorHex,
            paletteStyle = theme.paletteStyle,
            colorSpec = theme.colorSpec,
            uiStyle = theme.uiStyle,
            liquidGlass = theme.liquidGlass,
            // text sits on the cover-tinted backdrop, so keep the cover palette instead of greys
            neutralSurfaces = false
        ) {
            PredictiveDismissHandler(state = dismissState, onDismiss = onClose)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // phone morphs own the back preview, while tablets keep the default transform
                    .then(if (useContainerMorph) Modifier else Modifier.predictiveDismissTransform(dismissState))
                    .blockUnderlyingTouches()
            ) {
                NowPlayingBackdrop(
                    NowPlayingBackdropRequest(cover, queueFlow, background)
                )

                CompositionLocalProvider(LocalMiniPlayerHeight provides 0.dp) {
                    content()
                }
            }
        }
    }
    if (useContainerMorph) {
        val backPreviewEnabled = LocalPredictiveBackPreviewEnabled.current
        val morph = rememberNowPlayingMorphState(
            visible = visible,
            flight = sharedCoverFlight,
            artwork = { resolveNowPlayingOverlayArtwork(cover.songKey, sharedCoverFlight, cover.clearCover) },
            backProgress = {
                if (backPreviewEnabled) {
                    FastOutLinearInEasing.transform(dismissState.progress.coerceIn(0f, 1f))
                } else 0f
            }
        )
        if (morph.mounted) {
            NowPlayingContainerMorph(
                morph = morph,
                flight = sharedCoverFlight,
                capsuleInWindow = { sharedCoverFlight?.miniPlayerContainerBounds },
                cardColor = remember(theme.activeCoverSeedHex) {
                    nowPlayingMorphCardColor(theme.activeCoverSeedHex)
                },
                content = overlayBody
            )
        }
        return
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            initialOffsetY = { fullHeight -> fullHeight }
        ) + fadeIn(animationSpec = tween(durationMillis = 210)),
        exit = slideOutVertically(
            // 进出统一 350ms; 淡出比滑动略短, 避免整段过渡都处于半透明状态
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            targetOffsetY = { fullHeight -> fullHeight }
        ) + fadeOut(animationSpec = tween(durationMillis = 245))
    ) {
        overlayBody()
    }
}
