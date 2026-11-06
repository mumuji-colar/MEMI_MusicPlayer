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
 * File: moe.ouom.neriplayer.ui.component.navigation/NeriMochiTopBar
 * Created: 2026/11/2
 */

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import ui.feice.mochi.theme.MochiTheme

/**
 * 顶栏标题统一用 Mochi 的字阶。
 *
 * 各页现成的标题都是裸 `Text(...)`（不传 style），它默认读 `LocalTextStyle`，
 * 所以在这一层换掉字阶，全部顶栏标题就一起变了，不用逐个文件改标题写法。
 */
@Composable
private fun ProvideMochiTitleStyle(content: @Composable () -> Unit) {
    ProvideTextStyle(value = MochiTheme.textStyles.titleLarge, content = content)
}

/**
 * 顶栏的 Mochi 中间层。
 *
 * 这里刻意**没有**换成 `MochiTitleBar`：全站 30 多处顶栏都在用 `scrollBehavior` 做滚动折叠，
 * 而 `MochiTitleBar` 只收 `title: String` 和 progress lambda，硬换会把折叠行为、predictive back、
 * 自定义 insets 一起丢掉。所以内部仍走 Material 的 `TopAppBar`，只把标题字阶换成 Mochi 的
 * `titleLarge`，其余参数原样透传，行为与改造前完全一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NeriMochiTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets
) {
    TopAppBar(
        title = { ProvideMochiTitleStyle(title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight = expandedHeight,
        colors = colors,
        scrollBehavior = scrollBehavior,
        windowInsets = windowInsets
    )
}

/** [LargeTopAppBar] 的同款中间层，保留大标题折叠。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NeriMochiLargeTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.LargeAppBarExpandedHeight,
    collapsedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets
) {
    LargeTopAppBar(
        title = { ProvideMochiTitleStyle(title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight = expandedHeight,
        collapsedHeight = collapsedHeight,
        colors = colors,
        scrollBehavior = scrollBehavior,
        windowInsets = windowInsets
    )
}
