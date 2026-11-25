package com.mmc.merimusicplayer.lyrics.output

import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.lyrics.LyricEntry

internal interface LyriconOutputSink {
    fun updateSong(song: SongItem, lyrics: List<LyricEntry>?, translatedLyrics: List<LyricEntry>?, lyricOffsetMs: Long)
    fun setPlaybackState(playing: Boolean)
    fun setLyricOffset(offsetMs: Long)
    fun setPosition(positionMs: Long)
}
