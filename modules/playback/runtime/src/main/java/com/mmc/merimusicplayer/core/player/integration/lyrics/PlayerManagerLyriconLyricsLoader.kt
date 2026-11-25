package com.mmc.merimusicplayer.core.player.integration.lyrics

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.server.isServerSong
import com.mmc.merimusicplayer.data.model.lyrics.LyricEntry
import com.mmc.merimusicplayer.data.model.settings.lyrics.LyricSourcePreference
import com.mmc.merimusicplayer.lyrics.output.LyriconLyricsLoader

internal object PlayerManagerLyriconLyricsLoader : LyriconLyricsLoader {
    override suspend fun load(
        song: SongItem,
        preferredSource: LyricSourcePreference,
        publish: (List<LyricEntry>, List<LyricEntry>, LyricSourcePreference?) -> Unit,
    ) {
        val preferred = PlayerManager.getPreferredLyricSourceResult(song, preferredSource)
        val lyrics = try {
            preferred?.lyrics ?: PlayerManager.getLyrics(song, skipPreferredSource = true)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (!song.isServerSong()) throw error
            // The playback page owns retry feedback. A server outage must not crash this output job.
            return
        }
        currentCoroutineContext().ensureActive()
        val translatedLyrics = preferred?.translatedLyrics
            ?: PlayerManager.getTranslatedLyrics(song, skipPreferredSource = true)
        currentCoroutineContext().ensureActive()
        publish(lyrics, translatedLyrics, preferred?.source)
    }
}
