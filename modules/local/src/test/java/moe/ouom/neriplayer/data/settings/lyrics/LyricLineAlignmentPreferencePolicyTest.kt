package moe.ouom.neriplayer.data.settings.lyrics

import moe.ouom.neriplayer.data.model.settings.lyrics.DEFAULT_LYRIC_LINE_ALIGNMENT
import moe.ouom.neriplayer.data.model.settings.lyrics.LyricLineAlignment

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 歌词行对齐的存储值编解码。
 *
 * 默认值必须落在 [LyricLineAlignment.Follow]：这一档在渲染层等价于改动前的行为，
 * 存量用户升级后读不到这个键，会走默认值，画面不该发生变化。
 */
class LyricLineAlignmentPreferencePolicyTest {

    @Test
    fun defaultStorageValueIsFollow() {
        assertEquals("follow", DEFAULT_LYRIC_LINE_ALIGNMENT)
        assertEquals(
            LyricLineAlignment.Follow,
            LyricLineAlignmentPreferencePolicy.fromStorage(null)
        )
    }

    @Test
    fun blankValueFallsBackToDefault() {
        assertEquals(
            LyricLineAlignment.Follow,
            LyricLineAlignmentPreferencePolicy.fromStorage("")
        )
        assertEquals(
            LyricLineAlignment.Follow,
            LyricLineAlignmentPreferencePolicy.fromStorage("   ")
        )
    }

    @Test
    fun unknownValueFallsBackToDefault() {
        // 备份来自更高版本、或字段被手改过时不能崩，直接回落
        assertEquals(
            LyricLineAlignment.Follow,
            LyricLineAlignmentPreferencePolicy.fromStorage("justify")
        )
    }

    @Test
    fun centerValueRoundTrips() {
        val stored = LyricLineAlignmentPreferencePolicy.normalize("center")
        assertEquals("center", stored)
        assertEquals(
            LyricLineAlignment.Center,
            LyricLineAlignmentPreferencePolicy.fromStorage(stored)
        )
    }

    @Test
    fun parsingIsCaseAndWhitespaceInsensitive() {
        assertEquals(
            LyricLineAlignment.Center,
            LyricLineAlignmentPreferencePolicy.fromStorage("  CENTER ")
        )
    }

    @Test
    fun normalizeRewritesUnknownValueToDefault() {
        // 写入前必须先规范化，避免脏值落盘
        assertEquals(
            "follow",
            LyricLineAlignmentPreferencePolicy.normalize("something-else")
        )
    }

    @Test
    fun everyEntryHasDistinctStorageValue() {
        val values = LyricLineAlignment.entries.map { it.storageValue }
        assertEquals(values.size, values.toSet().size)
    }
}
