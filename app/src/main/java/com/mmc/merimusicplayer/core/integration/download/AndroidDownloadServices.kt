package com.mmc.merimusicplayer.core.integration.download

import com.mmc.merimusicplayer.core.di.AppContainer
import com.mmc.merimusicplayer.core.download.host.DownloadCredentials
import com.mmc.merimusicplayer.core.download.host.DownloadLyricServices
import com.mmc.merimusicplayer.core.download.host.DownloadSourceServices

internal object AndroidDownloadServices : DownloadSourceServices, DownloadLyricServices, DownloadCredentials {
    override val subsonicRepository get() = AppContainer.subsonicRepository
    override val neteaseClient get() = AppContainer.neteaseClient
    override val biliClient get() = AppContainer.biliClient
    override val biliPlaybackRepository get() = AppContainer.biliPlaybackRepository
    override val youtubeMusicPlaybackRepository get() = AppContainer.youtubeMusicPlaybackRepository
    override val youtubeMusicDownloadPlaybackRepository get() = AppContainer.youtubeMusicDownloadPlaybackRepository
    override val youtubeMusicClient get() = AppContainer.youtubeMusicClient
    override val lrcLibClient get() = AppContainer.lrcLibClient
    override fun biliCookies() = AppContainer.biliCookieRepo.getCookiesOnce()
    override fun youtubeAuth() = AppContainer.youtubeAuthRepo.getAuthOnce()
}
