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
 * File: moe.ouom.neriplayer.data.model.bilibili.mv/BiliMvTarget
 */

package moe.ouom.neriplayer.data.model.bilibili.mv

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 一首歌对应的 MV（B 站视频）身份。
 *
 * B 站音源的歌曲本身就是一个视频，所以「有没有 MV」等价于「能不能解析出 bvid」。
 * cid 为分 P 标识，缺失时由播放页现取（多 P 视频取第一 P）。
 */
@Parcelize
data class BiliMvTarget(
    val bvid: String,
    val cid: Long? = null,
    val title: String = "",
    val artist: String = ""
) : Parcelable
