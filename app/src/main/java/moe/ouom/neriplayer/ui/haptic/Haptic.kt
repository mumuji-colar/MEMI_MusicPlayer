package moe.ouom.neriplayer.ui.haptic

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.util/Haptic
 * Created: 2025/8/13
 */
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import ui.feice.mochi.anim.mochiSpring
import ui.feice.mochi.shape.MochiCapsule

private const val VIBRATION_EFFECT_CLICK = 0
private const val VIBRATION_EFFECT_DOUBLE_CLICK = 1
private const val VIBRATION_EFFECT_TICK = 2
private const val VIBRATION_EFFECT_HEAVY_CLICK = 5

enum class HapticFeedbackEffect(
    val predefinedEffect: Int,
    val fallbackDurationMs: Long,
    val fallbackAmplitude: Int
) {
    Tick(VIBRATION_EFFECT_TICK, 8L, 32),
    Click(VIBRATION_EFFECT_CLICK, 12L, 72),
    Confirm(VIBRATION_EFFECT_DOUBLE_CLICK, 20L, 96),
    Heavy(VIBRATION_EFFECT_HEAVY_CLICK, 24L, 160)
}

@Volatile
var hapticFeedbackEnabled: Boolean = true
    private set

fun syncHapticFeedbackSetting(enabled: Boolean) {
    hapticFeedbackEnabled = enabled
}

fun Context.performHapticFeedback(effect: HapticFeedbackEffect = HapticFeedbackEffect.Click) {
    if (!hapticFeedbackEnabled) return

    val vibrator = getSystemService(Vibrator::class.java) ?: return
    if (!vibrator.hasVibrator()) return

    runCatching {
        vibrator.vibrate(createVibrationEffect(vibrator, effect))
    }
}

private fun createVibrationEffect(
    vibrator: Vibrator,
    effect: HapticFeedbackEffect
): VibrationEffect {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        vibrator.supportsPredefinedEffect(effect.predefinedEffect)
    ) {
        return VibrationEffect.createPredefined(effect.predefinedEffect)
    }
    return VibrationEffect.createOneShot(effect.fallbackDurationMs, effect.fallbackAmplitude)
}

private fun Vibrator.supportsPredefinedEffect(effect: Int): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
    return isPredefinedEffectSupportUsable(areEffectsSupported(effect).firstOrNull())
}

@RequiresApi(Build.VERSION_CODES.R)
internal fun isPredefinedEffectSupportUsable(support: Int?): Boolean =
    support == Vibrator.VIBRATION_EFFECT_SUPPORT_YES ||
        support == Vibrator.VIBRATION_EFFECT_SUPPORT_UNKNOWN

private fun hapticClick(
    context: Context,
    effect: HapticFeedbackEffect,
    onClick: () -> Unit
): () -> Unit = {
    context.performHapticFeedback(effect)
    onClick()
}

/**
 * 按压回弹：手指按住时整个按钮会「鼓」一点，松手用弹簧弹回去。
 *
 * 方向是放大而不是缩小——和 Mochi 自己的 MochiButton 一致（它的 pressScale 是 1.08，
 * 用 MochiFollowSpring(0.26f, 0.6f) 跟手）。放大比缩小更像玻璃被顶起来，也是 iOS 26 那一套手感。
 *
 * 为什么这一档动效不心疼电：它完全由按压状态驱动，animateFloatAsState 只在值变化的那一小段时间里
 * 跑帧，停下之后不占 Choreographer。另外缩放写在 graphicsLayer 的 lambda 里，
 * 是绘制阶段读 state，整个回弹过程不触发一次重组，也不改布局尺寸（邻居不会跟着抖）。
 *
 * @param maxScale 按下时放大到的比例。按钮越宽取值越保守，否则整块会顶出边界。
 */
@Composable
private fun rememberPressBounce(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    maxScale: Float
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) maxScale else 1f,
        // 280ms + 0.28 回弹 ≈ Mochi 那套 spring(0.26s) 的手感，松手会轻微过冲一下再停住
        animationSpec = mochiSpring(
            durationMillis = 280,
            bounce = 0.28,
            visibilityThreshold = 0.001f
        ),
        label = "pressBounce"
    )
    return Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun HapticIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = hapticClick(LocalContext.current, HapticFeedbackEffect.Click, onClick),
        modifier = modifier.then(rememberPressBounce(interactionSource, enabled, 1.06f)),
        enabled = enabled,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun HapticFilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // 默认轮廓换成 Mochi 的连续胶囊：方形按钮看起来仍是圆，但曲率是苹果那种连续过渡，
    // 播放页/迷你播放器/歌词页的播放按钮都走这里，改一处全部跟上
    shape: Shape = MochiCapsule,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    FilledIconButton(
        onClick = hapticClick(LocalContext.current, HapticFeedbackEffect.Confirm, onClick),
        // 主播放键是全场唯一的「主角」，放大给足一点，手感最明显
        modifier = modifier.then(rememberPressBounce(interactionSource, enabled, 1.08f)),
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun HapticButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = hapticClick(LocalContext.current, HapticFeedbackEffect.Confirm, onClick),
        // 长条按钮只要一点点起伏，放大太多会溢出色块边界
        modifier = modifier.then(rememberPressBounce(interactionSource, enabled, 1.02f)),
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun HapticTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    TextButton(
        onClick = hapticClick(LocalContext.current, HapticFeedbackEffect.Click, onClick),
        modifier = modifier.then(rememberPressBounce(interactionSource, enabled, 1.02f)),
        enabled = enabled,
        shape = ButtonDefaults.shape,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun HapticOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = hapticClick(LocalContext.current, HapticFeedbackEffect.Click, onClick),
        modifier = modifier.then(rememberPressBounce(interactionSource, enabled, 1.02f)),
        enabled = enabled,
        shape = shape,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun HapticFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hapticEffect: HapticFeedbackEffect = HapticFeedbackEffect.Confirm,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    FloatingActionButton(
        onClick = hapticClick(LocalContext.current, hapticEffect, onClick),
        modifier = modifier.then(rememberPressBounce(interactionSource, true, 1.08f)),
        interactionSource = interactionSource,
        content = content
    )
}
