package com.mmc.merimusicplayer.platform.lyrics.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import com.mmc.merimusicplayer.platform.search.api.NativeLyricSearchApi
import com.mmc.merimusicplayer.platform.search.api.client.QQMusicSearchApi
import com.mmc.merimusicplayer.data.model.qqmusic.search.QQMusicSongMetadata
import com.mmc.merimusicplayer.common.logging.NPLogger
import com.mmc.merimusicplayer.data.model.music.SongDetails
import com.mmc.merimusicplayer.data.model.music.SongSearchInfo
import com.mmc.merimusicplayer.platform.lyrics.matching.chooseQQMusicLyrics

class QQMusicLyricsRepository(
    private val api: QQMusicSearchApi,
    private val amllTtmlClient: AmllLyricsRepository,
    private val amllLyricsEnabledProvider: suspend () -> Boolean
) : NativeLyricSearchApi {
    override suspend fun search(keyword: String, page: Int): List<SongSearchInfo> =
        api.search(keyword, page)

    override suspend fun getSongInfo(id: String): SongDetails = withContext(Dispatchers.IO) {
        val metadata = api.getSongMetadata(id)
        coroutineScope {
            val lyricDeferred = async { api.getNativeLyrics(id) }
            val amllLyricDeferred = async { fetchAmllWordLyricIfEnabled(metadata) }
            val (qqLyric, qqTranslatedLyric) = lyricDeferred.await()
            val (lyric, translatedLyric) = chooseQQMusicLyrics(
                qqLyric = qqLyric,
                qqTranslatedLyric = qqTranslatedLyric,
                amllLyric = amllLyricDeferred.await()
            )
            metadata.details.copy(lyric = lyric, translatedLyric = translatedLyric)
        }
    }

    override suspend fun getNativeSongInfo(id: String): SongDetails = api.getNativeSongInfo(id)

    suspend fun getSongMetadataOnly(id: String): SongDetails = api.getSongMetadata(id).details

    private suspend fun fetchAmllWordLyricIfEnabled(metadata: QQMusicSongMetadata): String? {
        return try {
            if (amllLyricsEnabledProvider()) {
                AmllLyricsResolver.loadRawByMetadata(
                    trackName = metadata.details.songName,
                    artistName = metadata.details.singer,
                    durationMs = metadata.durationMs,
                    amllTtmlClient = amllTtmlClient,
                    requireDurationMatch = metadata.durationMs > 0L
                )?.rawLyrics
            } else {
                null
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            NPLogger.d(TAG, "AMLL QQ lyric lookup failed: ${error.message}")
            null
        }
    }

    private companion object {
        const val TAG = "QQMusicLyricsRepository"
    }
}
