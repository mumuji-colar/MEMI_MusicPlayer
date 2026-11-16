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
 * File: moe.ouom.neriplayer.mv/BiliMvLauncher
 */

package moe.ouom.neriplayer.mv

import android.app.Activity
import android.content.Context
import android.content.Intent
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.model.bilibili.mv.BiliMvTarget
import moe.ouom.neriplayer.platform.bilibili.playback.mv.biliMvTargetOrNull

/** MV 入口的统一判定与跳转，避免各处重复拼 Intent。 */
object BiliMvLauncher {

    /** 只有 B 站音源且能拿到 BV 号才算「有 MV」。 */
    fun targetOf(song: SongItem?): BiliMvTarget? = song?.biliMvTargetOrNull()

    fun canOpenMv(song: SongItem?): Boolean = targetOf(song) != null

    fun open(context: Context, target: BiliMvTarget): Boolean = runCatching {
        val intent = Intent(context, BiliMvPlayerActivity::class.java).apply {
            putExtra(BiliMvPlayerActivity.EXTRA_TARGET, target)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.isSuccess

    fun openForSong(context: Context, song: SongItem?): Boolean {
        val target = targetOf(song) ?: return false
        return open(context, target)
    }
}
