/*
 * MEMI輕音 / MemiPlayer - A modified build of NeriPlayer.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * Copyright (C) 2026 MMC
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
 * File: moe.ouom.neriplayer.ui.startup/StartupBrandSplash
 */

package moe.ouom.neriplayer.ui.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.ouom.neriplayer.R as AppR
import moe.ouom.neriplayer.common.R as CoreCommonR

/**
 * 系统启动图之后的品牌承接页。
 *
 * 系统那张图（Android 12+ 的 SplashScreen）只能配图标和底色，放不下文字；
 * 这一页把底色和图标尺寸照着系统图对齐，再补上应用名，观感上就是「同一张启动图带了名字」。
 *
 * 深浅色规则刻意和系统启动图一致：深色纯黑底、浅色纯白底，避免冷启动时先闪一下别的颜色。
 */
@Composable
internal fun StartupBrandSplash(
    isDark: Boolean,
    showLoadingIndicator: Boolean,
    modifier: Modifier = Modifier
) {
    val background = if (isDark) Color.Black else Color.White
    val contentColor = if (isDark) {
        Color.White.copy(alpha = 0.92f)
    } else {
        Color.Black.copy(alpha = 0.88f)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            // 整体略上移：图标落在屏幕中心，应用名挂在下面，视觉重心不会偏下
            modifier = Modifier.offset(y = StartupBrandSplashDefaults.ContentOffsetY)
        ) {
            Image(
                painter = painterResource(AppR.drawable.ic_memi_splash),
                contentDescription = null,
                modifier = Modifier.size(StartupBrandSplashDefaults.IconSize)
            )
            Spacer(Modifier.height(StartupBrandSplashDefaults.IconToWordmark))
            Text(
                text = stringResource(CoreCommonR.string.app_name),
                style = TextStyle(
                    fontSize = StartupBrandSplashDefaults.WordmarkSize,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = StartupBrandSplashDefaults.WordmarkLetterSpacing,
                    color = contentColor
                )
            )
            if (showLoadingIndicator) {
                Spacer(Modifier.height(StartupBrandSplashDefaults.WordmarkToIndicator))
                CircularProgressIndicator(
                    modifier = Modifier.size(StartupBrandSplashDefaults.IndicatorSize),
                    color = contentColor.copy(alpha = 0.35f),
                    strokeWidth = 1.5.dp
                )
            }
        }
    }
}

/**
 * 品牌页的尺寸与停留时间。
 *
 * [IconSize] 是对着系统启动图肉眼调的一个值：系统把图标画在 288dp 容器里、
 * 内容占 2/3，实际落在屏幕上的直径各厂商略有差异，导入后如果发现衔接时图标跳了一下，
 * 只需要改这一个数。
 */
internal object StartupBrandSplashDefaults {
    val IconSize = 96.dp
    val IconToWordmark = 20.dp
    val WordmarkSize = 21.sp

    /** 21sp 下 0.02em ≈ 0.42sp */
    val WordmarkLetterSpacing = 0.4.sp
    val ContentOffsetY = (-24).dp

    val WordmarkToIndicator = 18.dp
    val IndicatorSize = 18.dp

    /** 品牌页最短停留时间；实测内容ready通常比这更慢，所以基本不额外拖慢启动 */
    const val MinDisplayMillis = 350L
}
