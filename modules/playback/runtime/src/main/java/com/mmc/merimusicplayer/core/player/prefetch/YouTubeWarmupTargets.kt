package com.mmc.merimusicplayer.core.player.prefetch

import com.mmc.merimusicplayer.platform.youtube.api.transport.extractYouTubeMusicVideoId
import com.mmc.merimusicplayer.core.player.runtime.prefetch.YouTubeWarmupTargets
import com.mmc.merimusicplayer.core.player.runtime.prefetch.resolveYouTubeImmediatePrefetchTargets
import com.mmc.merimusicplayer.core.player.runtime.prefetch.resolveYouTubePrefetchTargets
import com.mmc.merimusicplayer.data.model.SongItem

internal fun resolveYouTubeWarmupTargets(
    playlist: List<SongItem>,
    currentSongIndex: Int,
    preferredQuality: String
): YouTubeWarmupTargets {
    return resolveYouTubePrefetchTargets(playlist, currentSongIndex, preferredQuality) { song ->
        extractYouTubeMusicVideoId(song.mediaUri)
    }
}

internal fun resolveYouTubeImmediatePlaybackWarmupTargets(
    playlist: List<SongItem>,
    currentSongIndex: Int,
    preferredQuality: String
): YouTubeWarmupTargets {
    return resolveYouTubeImmediatePrefetchTargets(playlist, currentSongIndex, preferredQuality) { song ->
        extractYouTubeMusicVideoId(song.mediaUri)
    }
}
