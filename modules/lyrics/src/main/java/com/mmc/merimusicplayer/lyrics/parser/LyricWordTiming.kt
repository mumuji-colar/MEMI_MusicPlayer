package com.mmc.merimusicplayer.lyrics.parser

fun hasEditableLyricWordTiming(rawLyric: String): Boolean {
    if (rawLyric.isBlank()) {
        return false
    }
    return runCatching {
        parseNeteaseLyricsAuto(rawLyric).hasWordTimedEntries()
    }.getOrDefault(false)
}
