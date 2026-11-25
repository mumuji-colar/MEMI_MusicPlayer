package com.mmc.merimusicplayer.platform.youtube.media

import com.mmc.merimusicplayer.platform.youtube.api.transport.extractYouTubeMusicVideoId
import com.mmc.merimusicplayer.data.model.SongItem

fun isYouTubeMusicSong(song: SongItem): Boolean = extractYouTubeMusicVideoId(song.mediaUri) != null
