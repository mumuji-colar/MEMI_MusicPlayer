package com.mmc.merimusicplayer.core.player.playback

import com.mmc.merimusicplayer.core.player.runtime.progress.PlaybackProgressOwner
import com.mmc.merimusicplayer.core.player.runtime.progress.PlaybackProgressPort
import com.mmc.merimusicplayer.core.player.runtime.stats.PlaybackStatsTracker
import com.mmc.merimusicplayer.core.player.session.AppQueueSongIdentity
import com.mmc.merimusicplayer.data.model.SongIdentity
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.stableKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackIdentityIntegrationTest {
    @Test
    fun `app identity preserves listened time when a downloaded copy replaces the remote song`() {
        val remote = remoteSong()
        val local = downloadedCopy(remote)
        var now = 0L
        val tracker = PlaybackStatsTracker(AppQueueSongIdentity::stableKey, nowElapsedMs = { now })
        tracker.onSongChanged(remote)
        tracker.onPlayingChanged(true)
        now = 40_000L

        assertNull(tracker.onSongChanged(local))
        val snapshot = tracker.onPlayingChanged(false)!!
        assertEquals(local, snapshot.song)
        assertEquals(40_000L, snapshot.listenedMs)
        assertEquals(1, snapshot.playCountIncrement)
    }

    @Test
    fun `app identity backfills the current representation from a downloaded copy`() {
        val remote = remoteSong().copy(durationMs = 0L)
        val port = DurationPort(remote)
        val owner = PlaybackProgressOwner(port, AppQueueSongIdentity::sameIdentity) { 0L }

        owner.maybeUpdateSongDuration(downloadedCopy(remote), 60_000L)

        assertEquals(remote.copy(durationMs = 60_000L), port.current)
        assertEquals(60_000L, owner.duration.value)
    }

    private fun remoteSong() = SongItem(
        id = 1L,
        name = "song",
        artist = "artist",
        album = "Netease",
        albumId = 0L,
        durationMs = 60_000L,
        coverUrl = null,
        sourceStableKey = SongIdentity(1L, "netease", null).stableKey()
    )

    private fun downloadedCopy(song: SongItem) = song.copy(
        id = 2L,
        album = "本地",
        mediaUri = "content://example/document/song",
        localFilePath = "content://example/document/song"
    )

    private class DurationPort(var current: SongItem) : PlaybackProgressPort {
        override fun rememberLongFormEnabled() = false
        override fun rememberedPosition(song: SongItem) = 0L
        override fun writeRememberedPosition(song: SongItem, positionMs: Long) = Unit
        override fun currentSong() = current
        override fun reportedPositionMs() = 0L
        override fun playerPositionMs(): Long? = null
        override fun playerDurationMs(): Long? = null
        override fun updateQueuedDurationIfUnknown(song: SongItem, durationMs: Long) = false
        override fun replaceCurrentSong(song: SongItem) { current = song }
        override fun scheduleImmediateStatePersist() = Unit
        override fun pendingMediaLoadActive() = false
        override fun pendingMediaLoadPositionMs() = 0L
    }
}
