package com.mmc.merimusicplayer.core.player.host

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.local.media.source.ResolvedLocalMediaSource

import com.mmc.merimusicplayer.data.model.playback.storage.PlayerLocalPlaybackResolution
import com.mmc.merimusicplayer.data.model.playback.storage.PlayerDownloadedLyrics
import com.mmc.merimusicplayer.data.model.playback.storage.PlayerDownloadedArtwork
import com.mmc.merimusicplayer.data.model.playback.storage.PlayerMetadataClearRequest
import com.mmc.merimusicplayer.data.model.playback.storage.PlayerDownloadedMetadataSyncOutcome

interface PlayerDownloadAccess {
    val downloadPresenceVersion: StateFlow<Int>
    fun downloadedSongs(): List<SongItem>
    fun hasDownloadedSongCached(song: SongItem): Boolean
    fun findDownloadedSongCached(song: SongItem): PlayerDownloadedArtwork?
    fun isLikelyManagedDownloadSongFast(context: Context, song: SongItem): Boolean
    fun scanLocalFiles(context: Context, forceRefresh: Boolean)
    fun getLocalPlaybackUri(context: Context, song: SongItem): String?
    suspend fun resolvePermittedLocalPlaybackUri(context: Context, song: SongItem, rawLocalReference: String?, sourceContext: ResolvedLocalMediaSource? = null): String?
    suspend fun resolvePermittedLocalPlayback(context: Context, song: SongItem, rawLocalReference: String?, sourceContext: ResolvedLocalMediaSource? = null): PlayerLocalPlaybackResolution
    fun resolveIndexedLocalPlaybackReference(context: Context, song: SongItem): PlayerLocalPlaybackResolution
    fun hasRecentlyCompletedAudio(song: SongItem): Boolean
    fun invalidateCompletedAudioReference(song: SongItem)
    fun isSongDownloadActive(songKey: String): Boolean
    fun isMissingReferenceFailure(error: Throwable): Boolean
    fun peekLocalCoverUri(song: SongItem): String?
    suspend fun findCoverReferenceByFileName(context: Context, fileName: String, forceRefresh: Boolean, preferSidecarRefresh: Boolean): String?
    fun getLyricsBundleFast(context: Context, song: SongItem, allowColdSafProbe: Boolean = true): PlayerDownloadedLyrics
    fun getLyricContent(context: Context, song: SongItem): String?
    fun getTranslatedLyricContent(context: Context, song: SongItem): String?
    fun getRomanizedLyricContent(context: Context, song: SongItem): String?
    suspend fun syncDownloadedSongMetadataNow(song: SongItem, clearRestorableOverrides: PlayerMetadataClearRequest = PlayerMetadataClearRequest()): PlayerDownloadedMetadataSyncOutcome
    fun syncDownloadedSongMetadata(song: SongItem, clearRestorableOverrides: PlayerMetadataClearRequest = PlayerMetadataClearRequest())
}
