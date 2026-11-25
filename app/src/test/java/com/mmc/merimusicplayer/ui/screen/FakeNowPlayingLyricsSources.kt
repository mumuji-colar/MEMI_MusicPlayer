package com.mmc.merimusicplayer.ui.screen

import android.content.Context
import com.mmc.merimusicplayer.core.download.ManagedDownloadStorage
import com.mmc.merimusicplayer.core.player.metadata.PreferredLyricSourceResult
import com.mmc.merimusicplayer.data.local.media.LocalLyricsScanMetadata
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.settings.lyrics.LyricSourcePreference
import com.mmc.merimusicplayer.data.model.lyrics.LyricEntry
import com.mmc.merimusicplayer.ui.screen.nowplaying.lyrics.NowPlayingLyricsSources

internal open class FakeNowPlayingLyricsSources : NowPlayingLyricsSources {
    override fun hasManagedDownload(song: SongItem) = false
    override fun scheduleManagedRefresh(context: Context) = Unit
    override fun fastDownloaded(context: Context, song: SongItem): ManagedDownloadStorage.DownloadedLyricsBundle? = null
    override fun downloaded(context: Context, song: SongItem): ManagedDownloadStorage.DownloadedLyricsBundle? = null
    override fun inspectLocal(context: Context, song: SongItem, includeEmbedded: Boolean): LocalLyricsScanMetadata? = null
    override suspend fun preferred(song: SongItem, source: LyricSourcePreference): PreferredLyricSourceResult? = null
    override suspend fun neteaseOriginal(songId: Long) = ""
    override suspend fun neteaseRomanized(songId: Long) = ""
    override suspend fun onlineOriginal(song: SongItem): List<LyricEntry> = emptyList()
    override suspend fun onlineTranslated(song: SongItem): List<LyricEntry> = emptyList()
    override suspend fun onlineRomanized(song: SongItem): List<LyricEntry> = emptyList()
}
