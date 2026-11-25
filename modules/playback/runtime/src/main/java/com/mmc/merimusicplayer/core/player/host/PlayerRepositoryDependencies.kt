package com.mmc.merimusicplayer.core.player.host

import com.mmc.merimusicplayer.platform.subsonic.repository.SubsonicRepository

import com.mmc.merimusicplayer.platform.bilibili.api.client.BiliClient
import com.mmc.merimusicplayer.platform.netease.api.client.NeteaseClient
import com.mmc.merimusicplayer.platform.search.api.client.CloudMusicSearchApi
import com.mmc.merimusicplayer.platform.youtube.api.client.YouTubeMusicClient
import com.mmc.merimusicplayer.platform.bilibili.auth.BiliCookieRepository
import com.mmc.merimusicplayer.platform.netease.auth.NeteaseCookieRepository
import com.mmc.merimusicplayer.data.history.PlayHistoryRepository
import com.mmc.merimusicplayer.platform.lyrics.repository.AmllLyricsRepository
import com.mmc.merimusicplayer.platform.lyrics.repository.EditableLyricsMatcher
import com.mmc.merimusicplayer.platform.lyrics.repository.LrcLibLyricsRepository
import com.mmc.merimusicplayer.platform.lyrics.repository.QQMusicLyricsRepository
import com.mmc.merimusicplayer.platform.bilibili.playback.BiliPlaybackRepository
import com.mmc.merimusicplayer.platform.bilibili.skip.BiliVideoSkipRepository
import com.mmc.merimusicplayer.platform.bilibili.skip.sponsorblock.BiliSponsorBlockRepository
import com.mmc.merimusicplayer.data.playlist.usage.LocalPlaylistPlaybackStatsRepository
import com.mmc.merimusicplayer.data.playlist.usage.PlaylistUsageRepository
import com.mmc.merimusicplayer.data.settings.SettingsRepository
import com.mmc.merimusicplayer.data.stats.PlaybackStatsRepository
import com.mmc.merimusicplayer.data.traffic.TrafficStatsRepository
import com.mmc.merimusicplayer.platform.youtube.auth.YouTubeAuthRepository
import com.mmc.merimusicplayer.platform.youtube.repository.YouTubeMusicPlaybackRepository
import okhttp3.OkHttpClient

interface PlayerRepositoryDependencies {
    val subsonicRepository: SubsonicRepository? get() = null
    val settingsRepo: SettingsRepository
    val biliCookieRepo: BiliCookieRepository
    val neteaseCookieRepo: NeteaseCookieRepository
    val youtubeAuthRepo: YouTubeAuthRepository
    val biliClient: BiliClient
    val neteaseClient: NeteaseClient
    val youtubeMusicClient: YouTubeMusicClient
    val biliPlaybackRepository: BiliPlaybackRepository
    val youtubeMusicPlaybackRepository: YouTubeMusicPlaybackRepository
    val biliSponsorBlockRepository: BiliSponsorBlockRepository
    val biliVideoSkipRepository: BiliVideoSkipRepository
    val cloudMusicSearchApi: CloudMusicSearchApi
    val qqMusicSearchApi: QQMusicLyricsRepository
    val lrcLibClient: LrcLibLyricsRepository
    val amllTtmlClient: AmllLyricsRepository
    val editableLyricsMatcher: EditableLyricsMatcher
    val playHistoryRepo: PlayHistoryRepository
    val playlistUsageRepo: PlaylistUsageRepository
    val playbackStatsRepo: PlaybackStatsRepository
    val localPlaylistPlaybackStatsRepo: LocalPlaylistPlaybackStatsRepository
    val trafficStatsRepo: TrafficStatsRepository
    val sharedOkHttpClient: OkHttpClient
}

internal val settingsRepo get() = PlayerDependencies.repositories.settingsRepo
internal val biliCookieRepo get() = PlayerDependencies.repositories.biliCookieRepo
