package com.mmc.merimusicplayer.core.player.persistence

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import com.mmc.merimusicplayer.common.logging.NPLogger
import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.core.player.host.PlayerDependencies
import com.mmc.merimusicplayer.core.player.metadata.PlayerLyricsProvider
import com.mmc.merimusicplayer.core.player.metadata.SongMetadataApplyOptions
import com.mmc.merimusicplayer.core.player.metadata.SongMetadataApplyStage
import com.mmc.merimusicplayer.core.player.metadata.applySelectedSongMetadata
import com.mmc.merimusicplayer.core.player.metadata.fetchSelectedSongMetadataLyrics
import com.mmc.merimusicplayer.core.player.metadata.toBasicSongDetails
import com.mmc.merimusicplayer.data.identity.sameIdentityAs
import com.mmc.merimusicplayer.data.local.media.CustomSongCoverStorage
import com.mmc.merimusicplayer.data.local.media.LocalMediaMetadataWriteOutcome
import com.mmc.merimusicplayer.data.local.media.LocalMediaSupport
import com.mmc.merimusicplayer.data.local.media.LocalSongSupport
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.music.MusicPlatform
import com.mmc.merimusicplayer.data.model.music.SongSearchInfo
import com.mmc.merimusicplayer.data.model.playback.storage.PlayerDownloadedMetadataSyncOutcome

internal suspend fun PlayerManager.applyManualMetadataSelection(
    originalSong: SongItem,
    selectedSong: SongSearchInfo,
    options: SongMetadataApplyOptions,
    isLatestRequest: () -> Boolean,
    onProgress: suspend (SongMetadataApplyStage) -> Unit,
    onApplied: () -> Unit = {}
): Boolean {
    suspend fun report(stage: SongMetadataApplyStage) {
        currentCoroutineContext().ensureActive()
        if (!isLatestRequest()) throw CancellationException("metadata request superseded")
        onProgress(stage)
    }
    report(SongMetadataApplyStage.FETCHING_DETAILS)
    val details = try {
        when (selectedSong.source) {
            MusicPlatform.CLOUD_MUSIC -> PlayerDependencies.repositories.cloudMusicSearchApi.getSongMetadataOnly(selectedSong.id)
            MusicPlatform.QQ_MUSIC -> PlayerDependencies.repositories.qqMusicSearchApi.getSongMetadataOnly(selectedSong.id)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        NPLogger.w("NERI-PlayerManager", "Song detail lookup failed, using search summary: ${error.message.orEmpty()}")
        selectedSong.toBasicSongDetails()
    }
    if (options.fetchesLyrics) report(SongMetadataApplyStage.FETCHING_LYRICS)
    val lyrics = fetchSelectedSongMetadataLyrics(
        originalSong = originalSong,
        selectedSong = selectedSong,
        details = details,
        options = options,
        matchLyrics = { PlayerDependencies.repositories.editableLyricsMatcher.matchLyrics(it) },
        loadRomanized = { id ->
            PlayerLyricsProvider.getNeteaseRomanizedLyrics(id, neteaseClient, neteaseLyricsCache)
        }
    )
    report(SongMetadataApplyStage.PREPARING_COVER)
    val base = latestManualMetadataSong(originalSong)
    val isLocal = LocalSongSupport.isLocalSong(base, application)
    val cover = details.coverUrl?.takeIf(String::isNotBlank)?.let { reference ->
        if (isLocal && CustomSongCoverStorage.isRemoteReference(reference)) {
            CustomSongCoverStorage.persistManuallySelectedRemoteCover(application, reference)
                ?: return false
        } else reference
    }
    report(SongMetadataApplyStage.PREPARING_METADATA)
    val prepared = preserveLocalSongMetadataOriginals(
        song = base,
        preserveCover = cover != null,
        preserveLyrics = lyrics.hasUpdates
    )
    return runSongMetadataMutation {
        currentCoroutineContext().ensureActive()
        if (!isLatestRequest()) return@runSongMetadataMutation false
        // 取消只在准备阶段生效，文件提交和内存发布必须完成同一轮更新
        withContext(NonCancellable) {
            onProgress(SongMetadataApplyStage.APPLYING)
            if (!isLatestRequest()) return@withContext false
            val latest = latestManualMetadataSong(originalSong).withPreparedOriginals(prepared)
            val local = LocalSongSupport.isLocalSong(latest, application)
            val updated = applySelectedSongMetadata(
                originalSong = latest,
                selectedSong = selectedSong,
                details = details,
                coverReference = cover ?: latest.customCoverUrl ?: latest.coverUrl,
                lyrics = lyrics,
                useCustomOverride = local || PlayerDependencies.downloads.getLocalPlaybackUri(application, latest) != null,
                lyricRevision = if (lyrics.hasUpdates) localRepo.nextUserLyricSyncRevision(latest) else latest.lyricSyncRevision,
                captureMatchedOriginals = !local && lyrics.hasUpdates
            )
            if (local) {
                val coverReference = updated.customCoverUrl ?: cover
                val outcome = if (options.writeLocalMetadata) {
                    writeLocalEditableMetadata(
                        song = updated,
                        coverReference = coverReference,
                        writeCover = coverReference != null,
                        writeLyrics = lyrics.hasUpdates
                    )
                } else {
                    val writable = resolveLocalSidecarWriteSong(updated)
                    if (writable != null && LocalMediaSupport.writeLocalMetadataSidecar(
                            context = application,
                            song = writable,
                            writeLyrics = lyrics.hasUpdates,
                            coverReference = coverReference
                        )) LocalMediaMetadataWriteOutcome.SUCCESS else LocalMediaMetadataWriteOutcome.FAILED
                }
                if (outcome != LocalMediaMetadataWriteOutcome.SUCCESS) {
                    showLocalEditableMetadataWriteFeedback(outcome, PlayerDownloadedMetadataSyncOutcome.NOT_DOWNLOADED)
                    return@withContext false
                }
            }
            updateSongInAllPlaces(
                originalSong = latest,
                updatedSong = updated,
                triggerSync = true,
                syncDownloadedMetadata = shouldSyncDownloadedMetadataAfterMetadataUpdate(
                    writeLocalMetadata = options.writeLocalMetadata,
                    isLocalSong = local
                ),
                fastLocalUsageSync = true
            )
            onApplied()
            true
        }
    }
}

private fun PlayerManager.latestManualMetadataSong(song: SongItem): SongItem =
    currentQueueSnapshot().playlist.firstOrNull { it.sameIdentityAs(song) }
        ?: _currentSongFlow.value?.takeIf { it.sameIdentityAs(song) }
        ?: localRepo.playlists.value.firstNotNullOfOrNull { playlist ->
            playlist.songs.firstOrNull { it.sameIdentityAs(song) }
        }
        ?: song

private fun SongItem.withPreparedOriginals(prepared: SongItem): SongItem = copy(
    originalName = originalName ?: prepared.originalName,
    originalArtist = originalArtist ?: prepared.originalArtist,
    originalCoverUrl = originalCoverUrl ?: prepared.originalCoverUrl,
    originalLyric = originalLyric ?: prepared.originalLyric,
    originalTranslatedLyric = originalTranslatedLyric ?: prepared.originalTranslatedLyric,
    originalRomanizedLyric = originalRomanizedLyric ?: prepared.originalRomanizedLyric
)
