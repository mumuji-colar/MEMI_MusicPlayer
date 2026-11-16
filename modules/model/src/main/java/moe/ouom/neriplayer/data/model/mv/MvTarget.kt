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
 * File: moe.ouom.neriplayer.data.model.mv/MvTarget
 */

package moe.ouom.neriplayer.data.model.mv

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** MV 的来源平台。 */
enum class MvPlatform {
    /** B 站：歌曲本身就是一个视频。 */
    BILIBILI,

    /** 网易云：歌曲详情里的 `mv` 字段指向独立的 MV 资源。 */
    NETEASE,

    /** YouTube Music：歌曲本身就是一支视频，MV 就是同一支视频的画面。 */
    YOUTUBE
}

/**
 * 一首歌对应的 MV 身份，跨平台统一用这个结构在 Intent 里传递。
 *
 * [videoId] 对 B 站是 bvid，对网易云是 mvid（纯数字字符串）。
 * [cid] 只有 B 站有意义，缺失时由播放页现取。
 */
@Parcelize
data class MvTarget(
    val platform: MvPlatform,
    val videoId: String,
    val cid: Long? = null,
    val title: String = "",
    val artist: String = "",
    val coverUrl: String? = null
) : Parcelable

/**
 * 一个可直接播放的清晰度档位。
 *
 * [audioUrls] 为空表示音画合一（B 站 durl / 网易云 mp4），不能再拼音轨。
 * 同一首 MV 的不同档位共用 [MvStream.headers]。
 */
data class MvVariant(
    val height: Int,
    val label: String,
    val videoUrls: List<String>,
    val audioUrls: List<String> = emptyList(),
    val codecLabel: String = ""
)

/**
 * 解析完成、可以直接交给播放器的流信息。
 *
 * [variants] 按清晰度从高到低排列，切档位不用重新请求接口。
 * [headers] 是拉流时必须带上的请求头——B 站 CDN 按 UA 校验，网易云认 Referer。
 * [notice] 是给用户的一句话提醒（例如「这支没有 MV，只能放静态封面版」），没有就不显示。
 */
data class MvStream(
    val title: String,
    val variants: List<MvVariant>,
    val durationMs: Long? = null,
    val headers: Map<String, String> = emptyMap(),
    val notice: String? = null
) {
    /** 默认档位：不超过 1080P 的最高一档；全是超高清或没有分辨率信息时退回第一档。 */
    fun defaultVariantIndex(): Int {
        val index = variants.indexOfFirst { it.height in 1..1080 }
        return if (index >= 0) index else 0
    }

    /** 按用户记住的高度挑档位，没有同高度就退到不高于它的最高一档。 */
    fun variantIndexFor(preferredHeight: Int?): Int {
        if (variants.isEmpty()) return -1
        if (preferredHeight == null || preferredHeight <= 0) return defaultVariantIndex()
        val exact = variants.indexOfFirst { it.height == preferredHeight }
        if (exact >= 0) return exact
        val lower = variants.indexOfFirst { it.height in 1..preferredHeight }
        return if (lower >= 0) lower else variants.lastIndex
    }
}
