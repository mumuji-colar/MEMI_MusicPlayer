package com.mmc.merimusicplayer.core.player.lan

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.core.player.lyrics.getLyricsImpl
import com.mmc.merimusicplayer.core.player.lyrics.getRomanizedLyricsImpl
import com.mmc.merimusicplayer.core.player.lyrics.getTranslatedLyricsImpl
import com.mmc.merimusicplayer.data.local.media.displayName
import com.mmc.merimusicplayer.data.local.media.displayArtist
import com.mmc.merimusicplayer.data.local.media.displayAlbum
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.lan.*
import com.mmc.merimusicplayer.data.model.lyrics.LyricEntry
import com.mmc.merimusicplayer.data.model.lyrics.WordTiming

/** Uses the normal local/user-edited/cloud lyric resolution; never reparses into plain LRC. */
internal suspend fun resolveLanSharedLyrics(song: SongItem): LanSharedPresentation {
    // 房主沿正常来源查歌词，避免读取房间刚回传的封面或标题快照
    val lyrics = PlayerManager.getLyricsImpl(song, useLanPresentation = false)
    val translations = PlayerManager.getTranslatedLyricsImpl(song, useLanPresentation = false)
    val romanized = PlayerManager.getRomanizedLyricsImpl(song, useLanPresentation = false)
    return LanSharedPresentation(song.displayName(), song.displayArtist(), song.displayAlbum(PlayerManager.application),
        song.userLyricOffsetMs, lyrics.map(::shareLanLyricLine), translations.map(::shareLanLyricLine),
        romanized.map(::shareLanLyricLine))
}

internal fun shareLanLyricLine(line: LyricEntry): LanSharedLyricLine = LanSharedLyricLine(
    line.text, line.startTimeMs, line.endTimeMs,
    line.words?.map { LanSharedWordTiming(it.startTimeMs, it.endTimeMs, it.charCount) }, line.translation)

internal fun restoreLanLyricLine(line: LanSharedLyricLine): LyricEntry = LyricEntry(
    line.text, line.startTimeMs, line.endTimeMs,
    line.words?.map { WordTiming(it.startTimeMs, it.endTimeMs, it.charCount) }, line.translation)

/** In-memory session data only. The private received file is never a persisted library item. */
object LanSharedPresentationStore {
    data class Entry(val trackKey: String, val presentation: LanSharedPresentation)

    private val current = MutableStateFlow<Entry?>(null)
    val state = current.asStateFlow()

    fun publish(trackKey: String, value: LanSharedPresentation): Boolean {
        val next = Entry(trackKey, value)
        if (current.value == next) return false
        current.value = next
        return true
    }

    fun forSong(song: SongItem?, entry: Entry? = current.value): LanSharedPresentation? {
        if (entry == null) return null
        val trackKey = LanPlaybackBinding.track(song)?.stableKey
        return entry.presentation.takeIf { trackKey == entry.trackKey }
    }

    fun clear() { current.value = null }
}
