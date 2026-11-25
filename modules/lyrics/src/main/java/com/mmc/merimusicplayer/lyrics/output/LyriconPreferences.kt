package com.mmc.merimusicplayer.lyrics.output

import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.settings.lyrics.DEFAULT_AMLL_TTML_LYRIC_OFFSET_MS
import com.mmc.merimusicplayer.data.model.settings.lyrics.DEFAULT_BILI_CC_SUBTITLE_LYRIC_OFFSET_MS
import com.mmc.merimusicplayer.data.model.settings.lyrics.DEFAULT_CLOUD_MUSIC_LYRIC_OFFSET_MS
import com.mmc.merimusicplayer.data.model.settings.lyrics.DEFAULT_KUGOU_LYRIC_OFFSET_MS
import com.mmc.merimusicplayer.data.model.settings.lyrics.DEFAULT_LRCLIB_LYRIC_OFFSET_MS
import com.mmc.merimusicplayer.data.model.settings.lyrics.DEFAULT_QQ_MUSIC_LYRIC_OFFSET_MS
import com.mmc.merimusicplayer.data.model.settings.lyrics.LyricSourcePreference
import com.mmc.merimusicplayer.lyrics.offset.resolveEffectiveLyricOffsetMs

data class LyriconPreferences(
    val enabled: Boolean,
    val preferredSource: LyricSourcePreference = LyricSourcePreference.Automatic,
    val cloudMusicOffsetMs: Long = DEFAULT_CLOUD_MUSIC_LYRIC_OFFSET_MS,
    val qqMusicOffsetMs: Long = DEFAULT_QQ_MUSIC_LYRIC_OFFSET_MS,
    val kugouOffsetMs: Long = DEFAULT_KUGOU_LYRIC_OFFSET_MS,
    val lrcLibOffsetMs: Long = DEFAULT_LRCLIB_LYRIC_OFFSET_MS,
    val amllTtmlOffsetMs: Long = DEFAULT_AMLL_TTML_LYRIC_OFFSET_MS,
    val biliCcSubtitleOffsetMs: Long = DEFAULT_BILI_CC_SUBTITLE_LYRIC_OFFSET_MS,
) {
    internal fun offsetFor(
        song: SongItem,
        resolvedSource: LyricSourcePreference?,
        isBiliCcSubtitleLyrics: Boolean = false
    ): Long {
        return resolveEffectiveLyricOffsetMs(
            lyricSource = song.matchedLyricSource,
            cloudMusicDefaultOffsetMs = cloudMusicOffsetMs,
            qqMusicDefaultOffsetMs = qqMusicOffsetMs,
            userLyricOffsetMs = song.userLyricOffsetMs,
            kugouDefaultOffsetMs = kugouOffsetMs,
            lrclibDefaultOffsetMs = lrcLibOffsetMs,
            amllTtmlDefaultOffsetMs = amllTtmlOffsetMs,
            preferredLyricSource = resolvedSource,
            biliCcSubtitleDefaultOffsetMs = biliCcSubtitleOffsetMs,
            isBiliCcSubtitleLyrics = isBiliCcSubtitleLyrics,
        )
    }
}
