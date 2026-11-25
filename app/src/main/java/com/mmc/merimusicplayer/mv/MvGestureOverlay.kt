package com.mmc.merimusicplayer.mv

/*
 * MEMI輕音 / MemiPlayer - A modified build of MemiPlayer.
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
 * File: com.mmc.merimusicplayer.mv/MvGestureOverlay
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** 竖滑手势作用的对象：屏幕左半边调亮度，右半边调音量。 */
internal enum class MvGestureTarget { Brightness, Volume }

/** 手势过程中飘在画面上的提示，[fraction] 是 0..1 的目标值。 */
internal data class MvGestureHud(
    val target: MvGestureTarget,
    val fraction: Float
)

/**
 * 亮度/音量竖滑层。
 *
 * 只监听垂直拖动：水平拖动留给底部进度条，点击依旧冒泡给父级的「显示/隐藏控制栏」。
 * 手指在屏幕左半边按下就锁定亮度，右半边锁定音量，中途换边不改变目标。
 * 从底部滑到顶部大约等于走完 62% 屏高，和主流播放器手感接近。
 */
@Composable
internal fun MvGestureLayer(
    brightness: Float,
    volume: Float,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onHudChange: (MvGestureHud) -> Unit,
    modifier: Modifier = Modifier
) {
    // 手势自己维护当前值：连续两次拖动要从上次停下的位置继续，而不是跳回进入时的初值
    var currentBrightness by remember { mutableFloatStateOf(brightness.coerceIn(0f, 1f)) }
    var currentVolume by remember { mutableFloatStateOf(volume.coerceIn(0f, 1f)) }
    val latestOnBrightnessChange by rememberUpdatedState(onBrightnessChange)
    val latestOnVolumeChange by rememberUpdatedState(onVolumeChange)
    val latestOnHudChange by rememberUpdatedState(onHudChange)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var target: MvGestureTarget? = null
                var startValue = 0f
                var accumulated = 0f
                var travelPx = 1f
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val picked = if (offset.x < size.width / 2f) {
                            MvGestureTarget.Brightness
                        } else {
                            MvGestureTarget.Volume
                        }
                        target = picked
                        startValue = if (picked == MvGestureTarget.Brightness) {
                            currentBrightness
                        } else {
                            currentVolume
                        }
                        accumulated = 0f
                        travelPx = (size.height * GESTURE_TRAVEL_FRACTION).coerceAtLeast(1f)
                        latestOnHudChange(MvGestureHud(picked, startValue.coerceIn(0f, 1f)))
                    },
                    onVerticalDrag = { change, dragAmount ->
                        val current = target ?: return@detectVerticalDragGestures
                        change.consume()
                        accumulated += dragAmount
                        // 向上拖 dragAmount 为负，所以是减法
                        val fraction = (startValue - accumulated / travelPx).coerceIn(0f, 1f)
                        if (current == MvGestureTarget.Brightness) {
                            currentBrightness = fraction
                            latestOnBrightnessChange(fraction)
                        } else {
                            currentVolume = fraction
                            latestOnVolumeChange(fraction)
                        }
                        latestOnHudChange(MvGestureHud(current, fraction))
                    },
                    onDragEnd = { target = null },
                    onDragCancel = { target = null }
                )
            }
    )
}

/** 竖滑时显示的小卡片：图标 + 细进度条 + 百分比。 */
@Composable
internal fun MvGestureHudView(hud: MvGestureHud, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.72f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (hud.target == MvGestureTarget.Brightness) {
                    Icons.Filled.Brightness6
                } else {
                    Icons.Filled.VolumeUp
                },
                contentDescription = null,
                tint = Color.White
            )
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.24f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(hud.fraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
            Text(
                text = "${(hud.fraction.coerceIn(0f, 1f) * 100).roundToInt()}%",
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** 走完 100% 需要滑过的屏高比例。 */
private const val GESTURE_TRAVEL_FRACTION = 0.62f
