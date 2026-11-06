/*
 * MEMI輕音 / MemiPlayer - A modified build of NeriPlayer.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * Copyright (C) 2026 MMC
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
 * File: moe.ouom.neriplayer.data.model.settings.appearance/PerformanceMode
 */

package moe.ouom.neriplayer.data.model.settings.appearance

/**
 * 全局性能档位：作为特效总闸，在运行时对各特效做统一降级，
 * 不改变用户单独设置的各特效开关。
 */
enum class PerformanceMode(
    val storageValue: String
) {
    /** 省电：降低逐帧重绘频率与模糊精细度，优先压低功耗 */
    Saving("saving"),

    /** 均衡：默认档位，保持现状 */
    Balanced("balanced"),

    /** 画质：特效全开，不做任何降级 */
    Quality("quality")
}
