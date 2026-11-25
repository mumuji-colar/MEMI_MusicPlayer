package com.mmc.merimusicplayer.core.player.source

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
 * File: com.mmc.merimusicplayer.core.player.source/PlayerBiliVideoMapper
 * Updated: 2026/3/23
 */

import com.mmc.merimusicplayer.data.sync.mapping.toSongItem
import com.mmc.merimusicplayer.platform.bilibili.playback.resolver.buildBiliSongAlbum
import com.mmc.merimusicplayer.ui.viewmodel.playlist.BiliVideoItem
import com.mmc.merimusicplayer.data.model.SongItem

internal fun BiliVideoItem.toSongItem(): SongItem {
    return SongItem(
        id = id,
        name = title,
        artist = uploader,
        album = buildBiliSongAlbum(bvid = bvid),
        albumId = 0,
        durationMs = durationSec * 1000L,
        coverUrl = coverUrl,
        channelId = "bilibili",
        audioId = id.toString()
    )
}
