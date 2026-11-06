package moe.ouom.neriplayer.ui.component.navigation

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
 * File: moe.ouom.neriplayer.ui.component/NeriBottomBar
 * Created: 2025/8/8
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.animation.core.Animatable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import moe.ouom.neriplayer.ui.theme.LocalHasCustomBackground
import moe.ouom.neriplayer.ui.theme.NeriDesign
import moe.ouom.neriplayer.ui.theme.NeriMotion
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import moe.ouom.neriplayer.navigation.Destinations
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassRole
import moe.ouom.neriplayer.ui.effect.glass.AdvancedGlassSurface
import moe.ouom.neriplayer.ui.effect.glass.LocalAdvancedGlassController
import moe.ouom.neriplayer.ui.haptic.performHapticFeedback
import kotlinx.collections.immutable.toPersistentList
import ui.feice.mochi.basic.MochiTabBar
import ui.feice.mochi.basic.MochiTabBarDefaults
import ui.feice.mochi.basic.MochiTabItem

/**
 * Mochi 胶囊底板的内缩（`MochiTabBar` 里的私有常量 `BarPadding`）。
 * 胶囊宽度 = 每格宽度 × 格数 + 两侧内缩，玻璃要按同一个尺寸铺在下面。
 */
private val MochiTabCapsulePadding = 4.dp

/**
 * Mochi 胶囊底板的宽度：格数少时按内容收窄，放不下时贴满可用宽度。
 * `MochiTabBar` 内部用的是同一个公式，玻璃必须按它算尺寸才能和底板严丝合缝。
 */
internal fun mochiTabBarCapsuleWidth(
    availableWidth: Dp,
    itemWidth: Dp,
    itemCount: Int
): Dp = if (itemCount <= 0) {
    0.dp
} else {
    minOf(availableWidth, itemWidth * itemCount + MochiTabCapsulePadding * 2)
}

internal const val DEFAULT_BOTTOM_BAR_SELECTION_ALPHA = 0.72f
internal const val BOTTOM_BAR_FALLBACK_SCRIM_ALPHA = 0.28f

internal fun resolveBottomBarSelectionAlpha(
    hasCustomBackground: Boolean,
    alwaysUseNewTabStyle: Boolean
): Float = if (hasCustomBackground || alwaysUseNewTabStyle) {
    0f
} else {
    DEFAULT_BOTTOM_BAR_SELECTION_ALPHA
}

internal fun isNeriNavigationDestinationSelected(
    currentDestination: NavDestination?,
    destination: Destinations
): Boolean = currentDestination?.hierarchy?.any { it.route == destination.route } == true

@Composable
fun NeriBottomBar(
    items: List<Pair<Destinations, ImageVector>>,
    currentDestination: NavDestination?,
    onItemSelected: (Destinations) -> Unit,
    modifier: Modifier = Modifier,
    selectAlpha: Float = DEFAULT_BOTTOM_BAR_SELECTION_ALPHA
) {
    if (NeriDesign.isClean) {
        CleanFloatingTabBar(items, currentDestination, onItemSelected, modifier)
        return
    }
    // 标准风格走 Mochi：浮起胶囊 + 果冻选中块 + 按住滑选，配色由 NeriTheme 桥接过去
    MochiNavigationBar(
        items = items,
        currentDestination = currentDestination,
        onItemSelected = onItemSelected,
        modifier = modifier
    )
}

/**
 * [NeriBottomBar] 的 Mochi 版本。
 *
 * 标签、图标、跳转回调都跟原来一致，只是外观和手感换成 Mochi 的胶囊标签栏；
 * 选中态仍按 [isNeriNavigationDestinationSelected] 的层级判断，子页面里也能正确高亮。
 */
@Composable
private fun MochiNavigationBar(
    items: List<Pair<Destinations, ImageVector>>,
    currentDestination: NavDestination?,
    onItemSelected: (Destinations) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val labels = items.map { stringResource(it.first.labelResId) }
    val selectedIndex = items.indexOfFirst { (destination, _) ->
        isNeriNavigationDestinationSelected(currentDestination, destination)
    }
    // 只按 items 记忆：labels 每次重组都是新 List，用它当 key 会让缓存完全失效
    val mochiItems = remember(items) {
        items.mapIndexed { index, (destination, icon) ->
            MochiTabItem(
                label = labels[index],
                // 选中时用实心图标（原来是 Clean 风格的做法，这里沿用同一套映射）
                selectedIcon = { tint ->
                    Icon(
                        imageVector = cleanSelectedTabIcon(destination) ?: icon,
                        contentDescription = null,
                        tint = tint
                    )
                },
                icon = { tint ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint
                    )
                }
            )
        }.toPersistentList()
    }
    // 底栏和迷你播放器共用同一套高级玻璃：按 Mochi 的排布算出胶囊尺寸，
    // 把玻璃正好垫在胶囊下面，然后把 Mochi 自己的底板涂成透明。
    // 这样模糊开关、进阶模糊半径这两个设置对两条栏同时生效，外观也是同一种材质。
    val behavior = MochiTabBarDefaults.behavior()
    val mochiColors = MochiTabBarDefaults.colors()
    val capsuleColor = mochiColors.container
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            // 窗口内边距放到外层：玻璃量到的可用宽度才和 MochiTabBar 内部的 maxPx 一致
            .windowInsetsPadding(MochiTabBarDefaults.windowInsets),
        contentAlignment = Alignment.TopCenter
    ) {
        val capsuleWidth = mochiTabBarCapsuleWidth(
            availableWidth = maxWidth,
            itemWidth = behavior.itemWidth,
            itemCount = items.size
        )
        AdvancedGlassSurface(
            role = AdvancedGlassRole.BottomNavigation,
            modifier = Modifier.width(capsuleWidth).height(behavior.height),
            shape = CircleShape,
            tintColor = capsuleColor,
            fallbackColor = capsuleColor,
            content = {}
        )
        MochiTabBar(
            selectedIndex = selectedIndex.coerceAtLeast(0),
            onSelect = { index ->
                items.getOrNull(index)?.let { (destination, _) -> onItemSelected(destination) }
            },
            items = mochiItems,
            // 底板交给上面的玻璃画，这里只保留选中块、图标和融球
            colors = mochiColors.copy(container = Color.Transparent),
            behavior = behavior,
            // 内边距已由外层消费，再加一次会把胶囊顶离玻璃
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
/** Filled rounded glyph for the active tab, outlined for the rest, with a small spring pop. */
@Composable
internal fun CleanTabIcon(
    destination: Destinations,
    outlinedIcon: ImageVector,
    selected: Boolean,
    contentDescription: String
) {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            pop.snapTo(0.84f)
            pop.animateTo(1f, NeriMotion.bouncy())
        }
    }
    Icon(
        imageVector = if (selected) cleanSelectedTabIcon(destination) ?: outlinedIcon else outlinedIcon,
        contentDescription = contentDescription,
        modifier = Modifier.graphicsLayer {
            scaleX = pop.value
            scaleY = pop.value
        }
    )
}

internal fun cleanSelectedTabIcon(destination: Destinations): ImageVector? = when (destination.route) {
    Destinations.Home.route -> Icons.Rounded.Home
    Destinations.Explore.route -> Icons.Rounded.Search
    Destinations.Library.route -> Icons.Rounded.LibraryMusic
    Destinations.Settings.route -> Icons.Rounded.Settings
    Destinations.Debug.route -> Icons.Rounded.BugReport
    else -> null
}

internal fun shouldUseOpaqueBottomBarFallback(
    selectAlpha: Float,
    baseBlurRequested: Boolean
): Boolean = selectAlpha != 0f || baseBlurRequested

internal fun resolveBottomBarFallbackScrimAlpha(
    selectAlpha: Float,
    baseBlurRequested: Boolean
): Float = if (shouldUseOpaqueBottomBarFallback(selectAlpha, baseBlurRequested)) {
    BOTTOM_BAR_FALLBACK_SCRIM_ALPHA
} else {
    0f
}
