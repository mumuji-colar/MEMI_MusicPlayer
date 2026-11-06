package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun LyricsLineItem(
    isFocused: Boolean,
    /** 行起点系数：0f 贴左、0.5f 居中、1f 贴右。同时决定聚焦缩放的原点。 */
    horizontalBias: Float,
    onLineClicked: () -> Unit,
    onLinePressed: () -> Unit,
    blurRadius: () -> Float,
    modifier: Modifier = Modifier,
    focusedScale: Float = 1f,
    unfocusedScale: Float = 0.98f,
    activeAlpha: Float = 1f,
    inactiveAlpha: Float = 0.4f,
    blendMode: BlendMode = BlendMode.SrcOver,
    isInteractive: Boolean = true,
    /** 点击待确认时的高亮：只在本行绘制，不影响其它行 */
    isSelected: Boolean = false,
    selectionHighlightColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    val scaleState by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else unfocusedScale,
        animationSpec = if (isFocused) {
            tween(durationMillis = 600, easing = LinearOutSlowInEasing)
        } else {
            tween(durationMillis = 300, easing = EaseInOut)
        },
        label = "scale"
    )

    val alphaState by animateFloatAsState(
        targetValue = if (isFocused) activeAlpha else inactiveAlpha,
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.drawBehind {
                        val barWidth = 4.dp.toPx()
                        val inset = 8.dp.toPx()
                        drawRoundRect(
                            color = selectionHighlightColor,
                            topLeft = Offset(inset, inset),
                            size = Size(
                                barWidth,
                                (size.height - inset * 2).coerceAtLeast(barWidth)
                            ),
                            cornerRadius = CornerRadius(barWidth / 2f)
                        )
                    }
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                scaleX = scaleState
                scaleY = scaleState
                alpha = alphaState
                // 缩放原点必须跟着对齐走：贴左时原点在左下，居中时在中下，
                // 否则聚焦行的放大效果会往屏幕一侧甩。
                transformOrigin = TransformOrigin(horizontalBias, 1f)
                this.blendMode = blendMode
                compositingStrategy = CompositingStrategy.Offscreen

                val radius = blurRadius()
                if (radius > 0f) {
                    renderEffect = BlurEffect(
                        radiusX = radius,
                        radiusY = radius,
                        edgeTreatment = TileMode.Clamp
                    )
                }
            }
            .then(
                if (isInteractive) Modifier.clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = onLineClicked,
                        onLongClick = onLinePressed
                    )
                else Modifier
            )

    ) {
        content()
    }
}
