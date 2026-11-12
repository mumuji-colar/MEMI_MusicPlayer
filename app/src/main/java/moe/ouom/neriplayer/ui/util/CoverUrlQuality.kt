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
 * File: moe.ouom.neriplayer.ui.util/CoverUrlQuality
 */

package moe.ouom.neriplayer.ui.util

/**
 * 把各平台的封面地址换到高分辨率版本。
 *
 * 网易云的图片地址带 `?param=NyN` 参数，接口默认给 300×300，播放页再放大就糊了；
 * QQ 音乐用 `T002R300x300` 这种路径段表达尺寸。两边都换成 1080 级别，
 * 既够播放页那张大图用，也不至于拉一张几 MB 的原图。
 */
internal fun highResCoverUrl(url: String): String {
    if (url.isBlank()) return url
    return when {
        url.contains("music.126.net") -> {
            val replaced = url.replace(Regex("""\?param=\d+y\d+"""), "?param=1080y1080")
            if (replaced.contains("param=")) replaced else "$replaced?param=1080y1080"
        }

        url.contains("y.qq.com") || url.contains("y.gtimg.cn") ->
            url.replace(Regex("""T002R\d+x\d+"""), "T002R1200x1200")

        else -> url
    }
}
