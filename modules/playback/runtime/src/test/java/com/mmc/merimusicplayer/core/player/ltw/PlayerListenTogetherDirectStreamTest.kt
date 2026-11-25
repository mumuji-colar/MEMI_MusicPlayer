package com.mmc.merimusicplayer.core.player.ltw

import com.mmc.merimusicplayer.core.player.PlayerManager
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.playback.PlaybackUrlCandidate
import com.mmc.merimusicplayer.data.model.ltw.track.ListenTogetherTrack
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerListenTogetherDirectStreamTest {
    @Test
    fun `local direct stream bridge rejects missing mismatched and preview playback`() {
        val previousSong = PlayerManager._currentSongFlow.value
        val previousUrl = PlayerManager._currentMediaUrl.value
        val previousCandidates = PlayerManager.activePlaybackCandidates
        val previousIndex = PlayerManager.activePlaybackUrlIndex
        val song = SongItem(
            id = 42, name = "song", artist = "artist", album = "NeteaseAlbum", albumId = 1,
            durationMs = 60_000, coverUrl = null, channelId = "netease", audioId = "42",
            streamUrl = "https://example.com/audio.flac"
        )
        val track = ListenTogetherTrack("netease|42", "netease", "42", name = "song", artist = "artist")
        try {
            PlayerManager._currentSongFlow.value = null
            PlayerManager._currentMediaUrl.value = null
            PlayerManager.activePlaybackCandidates = emptyList()
            PlayerManager.activePlaybackUrlIndex = 0
            assertFalse(PlayerManagerListenTogetherHost.hasUsableLocalDirectStream(track))
            PlayerManager._currentSongFlow.value = song
            assertTrue(PlayerManagerListenTogetherHost.hasUsableLocalDirectStream(track))
            assertFalse(PlayerManagerListenTogetherHost.hasUsableLocalDirectStream(track.copy(audioId = "43")))
            PlayerManager._currentSongFlow.value = song.copy(streamUrl = null)
            assertFalse(PlayerManagerListenTogetherHost.hasUsableLocalDirectStream(track))
            PlayerManager._currentMediaUrl.value = "https://example.com/audio.flac"
            assertTrue(PlayerManagerListenTogetherHost.hasUsableLocalDirectStream(track))
            PlayerManager.activePlaybackCandidates = listOf(PlaybackUrlCandidate("https://example.com/preview.flac", isPreviewClip = true))
            assertFalse(PlayerManagerListenTogetherHost.hasUsableLocalDirectStream(track))
        } finally {
            PlayerManager._currentSongFlow.value = previousSong
            PlayerManager._currentMediaUrl.value = previousUrl
            PlayerManager.activePlaybackCandidates = previousCandidates
            PlayerManager.activePlaybackUrlIndex = previousIndex
        }
    }
}
