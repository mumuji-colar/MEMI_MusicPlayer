package com.mmc.merimusicplayer.core.player.playback

import android.content.Context
import com.mmc.merimusicplayer.core.player.runtime.stats.PlaybackStatsSnapshot
import com.mmc.merimusicplayer.core.player.runtime.stats.PlaybackStatsWritePort
import com.mmc.merimusicplayer.core.player.runtime.stats.countedLocalPlaylistId
import com.mmc.merimusicplayer.core.player.host.PlayerDependencies
import com.mmc.merimusicplayer.data.stats.PlaybackStatsRepository
import com.mmc.merimusicplayer.data.playlist.usage.LocalPlaylistPlaybackStatsRepository

internal object AppPlaybackStatsWritePort : PlaybackStatsWritePort {
    @Volatile private var application: Context? = null

    fun bind(context: Context) { application = context.applicationContext ?: context }

    private fun playbackRepository(): PlaybackStatsRepository = if (PlayerDependencies.isInitialized()) {
        PlayerDependencies.repositories.playbackStatsRepo
    } else application?.let(PlaybackStatsRepository::getInstance) ?: PlayerDependencies.repositories.playbackStatsRepo

    private fun playlistRepository(): LocalPlaylistPlaybackStatsRepository = if (PlayerDependencies.isInitialized()) {
        PlayerDependencies.repositories.localPlaylistPlaybackStatsRepo
    } else application?.let(LocalPlaylistPlaybackStatsRepository::getInstance) ?: PlayerDependencies.repositories.localPlaylistPlaybackStatsRepo

    override suspend fun record(snapshot: PlaybackStatsSnapshot) {
        playbackRepository().recordListenDeltaNow(
            song = snapshot.song,
            listenedMs = snapshot.listenedMs,
            playCountIncrement = snapshot.playCountIncrement,
            scheduleSync = snapshot.scheduleSync,
            eventId = snapshot.eventId,
            playedAt = snapshot.playedAt,
            observedClearedAt = snapshot.observedClearedAt
        )
        countedLocalPlaylistId(snapshot)?.let { playlistId ->
            playlistRepository().recordPlayNow(playlistId, snapshot.playedAt, snapshot.eventId)
        }
    }

    override fun hasPendingWrites(): Boolean = playbackRepository().hasPendingWrites()

    fun clearedAt(): Long = playbackRepository().statsClearedAtFlow.value

    override suspend fun flushPendingWrites() {
        playbackRepository().flushPendingWrites()
    }
}
