package com.mmc.merimusicplayer.core.download.host

import com.mmc.merimusicplayer.platform.netease.api.client.NeteaseClient
import com.mmc.merimusicplayer.platform.youtube.api.client.YouTubeMusicClient
import com.mmc.merimusicplayer.platform.lyrics.repository.LrcLibLyricsRepository

interface DownloadLyricServices {
    val neteaseClient: NeteaseClient
    val youtubeMusicClient: YouTubeMusicClient
    val lrcLibClient: LrcLibLyricsRepository
}
