package com.mmc.merimusicplayer.platform.subsonic.repository

import com.mmc.merimusicplayer.data.model.lyrics.LyricEntry
import com.mmc.merimusicplayer.lyrics.parser.convertPlainLyricsToEntries
import com.mmc.merimusicplayer.lyrics.parser.parseNeteaseLyricsAuto
import org.json.JSONObject

internal fun parseLegacyServerLyrics(json: JSONObject, durationMs: Long): List<LyricEntry> {
    val text = (json.optJSONObject("lyrics")?.opt("value") as? String)?.trim()
        ?.takeIf { it.isNotEmpty() } ?: return emptyList()
    val timed = parseNeteaseLyricsAuto(text)
    return timed.takeIf { entries -> entries.any { it.text.isNotBlank() } }
        ?: convertPlainLyricsToEntries(text, durationMs)
}
