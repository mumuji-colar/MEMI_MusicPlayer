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
 * File: moe.ouom.neriplayer.mv/MvLauncher
 */

package moe.ouom.neriplayer.mv

import android.app.Activity
import android.content.Context
import android.content.Intent
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.data.model.mv.MvTarget
import moe.ouom.neriplayer.platform.bilibili.playback.mv.biliMvTargetOrNull
import moe.ouom.neriplayer.platform.netease.mv.neteaseMvTargetOrNull

/** MV 入口的统一判定与跳转，避免各处重复拼 Intent。 */
object MvLauncher {

    /** B 站音源自带 BV 号，同步就能判定；网易云要现查 mv id，走 [resolveTarget]。 */
    fun targetOf(song: SongItem?): MvTarget? = song?.biliMvTargetOrNull()

    fun canOpenMv(song: SongItem?): Boolean = targetOf(song) != null

    /**
     * 补齐需要联网确认的 MV 身份（目前只有网易云）。
     * 拿不到（没 MV / 请求失败）返回 null，调用方据此决定角标是否出现。
     */
    suspend fun resolveTarget(song: SongItem?): MvTarget? {
        if (song == null) return null
        targetOf(song)?.let { return it }
        if (!song.channelId.equals("netease", ignoreCase = true)) return null
        val mvId = AppContainer.neteaseMvRepository.mvIdOf(song.id) ?: return null
        return song.neteaseMvTargetOrNull(mvId)
    }

    suspend fun hasMv(song: SongItem?): Boolean {
        if (song == null) return false
        return canOpenMv(song) || resolveTarget(song) != null
    }

    fun open(context: Context, target: MvTarget): Boolean = runCatching {
        val intent = Intent(context, MvPlayerActivity::class.java).apply {
            putExtra(MvPlayerActivity.EXTRA_TARGET, target)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.isSuccess
}
