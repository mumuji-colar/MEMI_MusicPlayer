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
 * File: moe.ouom.neriplayer.ui.component.common/MvBadge
 */

package moe.ouom.neriplayer.ui.component.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 歌单/播放页通用的「这首歌有 MV」标记，B 站与网易云共用。
 * 传 onClick 时变成可点的按钮，否则只是标识，不拦截列表行的点击。
 */
@Composable
internal fun MvBadge(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val accent = MaterialTheme.colorScheme.primary
    val container = modifier
        .clip(RoundedCornerShape(4.dp))
        .background(accent.copy(alpha = 0.16f))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 5.dp, vertical = 1.dp)

    Text(
        text = "MV",
        color = accent,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = container
    )
}
