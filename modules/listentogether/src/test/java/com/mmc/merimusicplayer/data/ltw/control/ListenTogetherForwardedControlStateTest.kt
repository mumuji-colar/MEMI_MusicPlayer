package com.mmc.merimusicplayer.data.ltw.control

import com.mmc.merimusicplayer.data.model.ltw.message.event.ListenTogetherEvent
import com.mmc.merimusicplayer.data.model.ltw.message.socket.ListenTogetherSocketEnvelope
import com.mmc.merimusicplayer.data.model.ltw.room.ListenTogetherRoomState
import com.mmc.merimusicplayer.data.model.ltw.track.ListenTogetherTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class ListenTogetherForwardedControlStateTest {
    @Test
    fun `track selection resolves committed legacy target without message track`() {
        val first = track("first")
        val next = track("next")
        val state = ListenTogetherRoomState(roomId = "room", version = 1L, queue = listOf(first, next), currentIndex = 0)
        val message = ListenTogetherSocketEnvelope(type = "control_requested")
        val selected = buildListenTogetherForwardedControlSyntheticState(state, message, ListenTogetherEvent(type = "SET_TRACK", track = next))
        assertEquals(1, selected.currentIndex)
        val unchanged = buildListenTogetherForwardedControlSyntheticState(state, message, ListenTogetherEvent(type = "SET_TRACK"))
        assertEquals(0, unchanged.currentIndex)
        val messageTarget = buildListenTogetherForwardedControlSyntheticState(state, message.copy(track = next), ListenTogetherEvent(type = "SET_TRACK", track = first))
        assertEquals(1, messageTarget.currentIndex)
    }

    private fun track(key: String) = ListenTogetherTrack(stableKey = key, channelId = "netease", audioId = key, name = key, artist = "artist")
}
