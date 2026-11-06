package moe.ouom.neriplayer.ui.theme

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
 * File: moe.ouom.neriplayer.ui.theme/MochiThemeBridge
 * Created: 2026/11/2
 */

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ui.feice.mochi.theme.MochiColorScheme
import ui.feice.mochi.theme.MochiTheme as MochiComposeTheme

/**
 * 把 NeriPlayer 现成的 Material 配色原样喂给 Mochi 组件。
 *
 * 改这一处，所有 Mochi 组件（标签栏、以后接的按钮/开关/列表）都跟着应用主题走：
 * 动态取色、种子色、深浅色、Clean 风格的降饱和配色都不用在 Mochi 侧重算一遍。
 */
@Composable
internal fun NeriMochiTheme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit
) {
    val mochiColors = remember(colorScheme) { colorScheme.toMochiColorScheme() }
    MochiComposeTheme(colors = mochiColors, content = content)
}

private const val DarkBackgroundLuminanceThreshold = 0.5f

private fun ColorScheme.toMochiColorScheme(): MochiColorScheme = MochiColorScheme(
    isDark = background.luminance() < DarkBackgroundLuminanceThreshold,
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = inversePrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    surfaceTint = surfaceTint,
    inverseSurface = inverseSurface,
    inverseOnSurface = inverseOnSurface,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    outline = outline,
    outlineVariant = outlineVariant,
    scrim = scrim,
    // Material3 的 ColorScheme 没有 shadow 角色，Mochi 这边固定用纯黑（规范里阴影本来就是黑的）
    shadow = Color.Black,
    surfaceBright = surfaceBright,
    surfaceDim = surfaceDim,
    surfaceContainerLowest = surfaceContainerLowest,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest
)
