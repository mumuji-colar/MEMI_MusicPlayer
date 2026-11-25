package com.mmc.merimusicplayer.ui.screen.nowplaying.edit

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mmc.merimusicplayer.core.download.GlobalDownloadManager
import com.mmc.merimusicplayer.core.download.ManagedDownloadStorage
import com.mmc.merimusicplayer.data.model.download.ManagedDownloadRestorableMetadata
import com.mmc.merimusicplayer.core.player.download.AudioDownloadManager
import com.mmc.merimusicplayer.data.local.media.LocalLyricsScanMetadata
import com.mmc.merimusicplayer.data.local.media.LocalMediaSupport
import com.mmc.merimusicplayer.data.local.media.LocalMetadataSidecar
import com.mmc.merimusicplayer.data.local.media.isLocalSong
import com.mmc.merimusicplayer.data.model.SongItem

internal interface EditSongBaselineReader {
    suspend fun metadata(context: Context, song: SongItem): ManagedDownloadRestorableMetadata?
    suspend fun cover(context: Context, metadata: ManagedDownloadRestorableMetadata): String?
    suspend fun sidecar(context: Context, song: SongItem): ManagedDownloadStorage.DownloadedLyricsBundle?
    suspend fun local(context: Context, song: SongItem): LocalLyricsScanMetadata?
}

internal object DefaultEditSongBaselineReader : EditSongBaselineReader {
    override suspend fun metadata(context: Context, song: SongItem) =
        readEditSongRestoreValue { GlobalDownloadManager.readManagedRestorableMetadata(context, song) }
            ?: song.takeIf { it.isLocalSong() }?.let {
                LocalMediaSupport.readLocalMetadataSidecarFast(context, it)
                    ?.toEditSongRestorableMetadata()
            }

    override suspend fun cover(context: Context, metadata: ManagedDownloadRestorableMetadata) =
        GlobalDownloadManager.resolveManagedRestorableCoverReference(
            context = context,
            metadata = metadata,
            baseline = true
        )

    override suspend fun sidecar(context: Context, song: SongItem) =
        AudioDownloadManager.getLyricsBundle(context, song)

    override suspend fun local(context: Context, song: SongItem) =
        LocalMediaSupport.inspectLyricsFast(
            context = context,
            song = song,
            includeStoredFallback = false,
            includeEmbeddedFallback = false,
            forceRefresh = true
        )
}

internal fun LocalMetadataSidecar.toEditSongRestorableMetadata() = ManagedDownloadRestorableMetadata(
    sourceStableKey = stableKey,
    baseline = ManagedDownloadRestorableMetadata.Baseline(
        title = originalName ?: name,
        artist = originalArtist ?: artist,
        album = album,
        coverReference = originalCoverUrl,
        originalLyric = originalLyric,
        translatedLyric = originalTranslatedLyric,
        romanizedLyric = originalRomanizedLyric
    ),
    overrides = ManagedDownloadRestorableMetadata.Overrides(
        title = customName,
        artist = customArtist,
        coverReference = customCoverUrl,
        originalLyric = matchedLyric,
        translatedLyric = matchedTranslatedLyric,
        romanizedLyric = matchedRomanizedLyric
    )
)

internal data class ManagedEditSongBaselineSnapshot(
    val metadata: ManagedDownloadRestorableMetadata,
    val coverReference: String?,
    val sidecarLyrics: ManagedDownloadStorage.DownloadedLyricsBundle?
)

internal suspend fun readManagedEditSongBaselineSnapshot(
    context: Context,
    song: SongItem,
    reader: EditSongBaselineReader = DefaultEditSongBaselineReader,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): ManagedEditSongBaselineSnapshot? = withContext(dispatcher) {
    val metadata = readEditSongRestoreValue { reader.metadata(context, song) }
        ?: return@withContext null
    val cover = readEditSongRestoreValue { reader.cover(context, metadata) }
    val sidecar = if (song.isLocalSong()) {
        readEditSongRestoreValue { reader.sidecar(context, song) }
    } else null
    ManagedEditSongBaselineSnapshot(metadata, cover, sidecar)
}

internal suspend fun resolveManagedEditSongBaselineAtRestore(
    context: Context,
    song: SongItem,
    current: EditSongBaseline,
    reader: EditSongBaselineReader = DefaultEditSongBaselineReader
): EditSongBaseline {
    if (!song.isLocalSong()) return current
    val metadata = readEditSongRestoreValue { reader.metadata(context, song) }
    val cover = metadata?.let { readEditSongRestoreValue { reader.cover(context, it) } }
    val sidecar = readEditSongRestoreValue { reader.sidecar(context, song) }
    val local = if (sidecar == null) readEditSongRestoreValue { reader.local(context, song) } else null
    return resolveManagedEditSongBaseline(
        current = current,
        metadata = metadata,
        coverReference = cover,
        sidecarLyrics = sidecar ?: local?.asDownloadedLyricsBundle(),
        retainedOriginals = song
    )
}

private suspend inline fun <T> readEditSongRestoreValue(
    crossinline block: suspend () -> T
): T? = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (_: Exception) {
    null
}

private fun LocalLyricsScanMetadata.asDownloadedLyricsBundle() =
    ManagedDownloadStorage.DownloadedLyricsBundle(
        lyric = lyric,
        translatedLyric = translatedLyric,
        romanizedLyric = romanizedLyric,
        hasOriginalSidecar = hasOriginalSidecar,
        hasTranslatedSidecar = hasTranslatedSidecar,
        hasRomanizedSidecar = hasRomanizedSidecar
    )
