package com.mmc.merimusicplayer.core.player.session

import com.mmc.merimusicplayer.data.model.stableKey
import androidx.media3.common.Player
import com.mmc.merimusicplayer.data.model.playback.queue.PlayerQueueSnapshot
import com.mmc.merimusicplayer.data.model.playback.queue.QueueInsertPlacement
import com.mmc.merimusicplayer.core.player.queue.policy.PlayerQueueEditOwner
import com.mmc.merimusicplayer.core.player.queue.policy.QueueRepeatMode
import com.mmc.merimusicplayer.core.player.queue.state.PlayerQueueStateStore
import com.mmc.merimusicplayer.data.model.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppQueueSongIdentityTest {
    @Test
    fun `local aliases retain same-source matching despite different stable keys`() {
        val original = SongItem(
            10L, "Local", "Artist", "Local Files", 0L, 100L, null,
            mediaUri = "content://media/external/audio/media/42", channelId = "local", audioId = "10"
        )
        val hydrated = original.copy(id = 20L, localFilePath = "/music/local.mp3", audioId = "20")
        assertNotEquals(AppQueueSongIdentity.stableKey(original), AppQueueSongIdentity.stableKey(hydrated))
        assertTrue(AppQueueSongIdentity.sameIdentity(original, hydrated))
        assertFalse(AppQueueSongIdentity.sameIdentity(original, null))

        val store = PlayerQueueStateStore(AppQueueSongIdentity).also { it.publish(listOf(original), 0) }
        store.updateSongMatching(hydrated) { it.copy(name = "Updated") }
        assertEquals("Updated", store.snapshot().playlist.single().name)

        val edit = PlayerQueueEditOwner(AppQueueSongIdentity).insert(
            PlayerQueueSnapshot.from(listOf(original), 0), hydrated, original, QueueInsertPlacement.END
        )
        assertEquals(listOf(hydrated), edit?.queue?.playlist)
        assertEquals(0, edit?.queue?.currentIndex)
    }

    @Test
    fun `queue repeat modes preserve the Media3 command values`() {
        assertEquals(Player.REPEAT_MODE_OFF, QueueRepeatMode.OFF)
        assertEquals(Player.REPEAT_MODE_ONE, QueueRepeatMode.ONE)
        assertEquals(Player.REPEAT_MODE_ALL, QueueRepeatMode.ALL)
    }
}
