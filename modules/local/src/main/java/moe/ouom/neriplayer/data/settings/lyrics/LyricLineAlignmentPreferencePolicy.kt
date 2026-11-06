package moe.ouom.neriplayer.data.settings.lyrics

import moe.ouom.neriplayer.data.model.settings.lyrics.LyricLineAlignment

import java.util.Locale

/**
 * 歌词行对齐的存储值规范化。
 *
 * 存量数据里没有这个键，读到 null 时回落到 [LyricLineAlignment.Follow]，
 * 保证升级后画面和改动前一致。
 */
object LyricLineAlignmentPreferencePolicy {
    fun normalize(value: String): String = fromStorage(value).storageValue

    fun fromStorage(value: String?): LyricLineAlignment {
        val normalized = value.orEmpty().trim().lowercase(Locale.ROOT)
        return LyricLineAlignment.entries.firstOrNull { it.storageValue == normalized }
            ?: LyricLineAlignment.DEFAULT
    }
}
