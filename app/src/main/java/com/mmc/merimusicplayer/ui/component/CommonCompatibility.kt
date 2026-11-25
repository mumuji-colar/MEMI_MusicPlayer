package com.mmc.merimusicplayer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.mmc.merimusicplayer.ui.component.common.blockUnderlyingTouches as newBlockUnderlyingTouches

fun Modifier.blockUnderlyingTouches(): Modifier =
    newBlockUnderlyingTouches()

@Composable
fun ThemeRevealOverlay(
    snapshot: ImageBitmap?,
    fallbackColor: Color,
    originInWindow: Offset,
    modifier: Modifier = Modifier,
    startRadiusPx: Float = 1f,
    reverse: Boolean = false,
    durationMillis: Int = 720,
    onFinished: () -> Unit
) {
    com.mmc.merimusicplayer.ui.component.common.ThemeRevealOverlay(
        snapshot = snapshot,
        fallbackColor = fallbackColor,
        originInWindow = originInWindow,
        modifier = modifier,
        startRadiusPx = startRadiusPx,
        reverse = reverse,
        durationMillis = durationMillis,
        onFinished = onFinished
    )
}
