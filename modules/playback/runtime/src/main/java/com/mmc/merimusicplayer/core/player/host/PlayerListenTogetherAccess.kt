package com.mmc.merimusicplayer.core.player.host

import kotlinx.coroutines.flow.StateFlow
import com.mmc.merimusicplayer.data.model.ltw.room.ListenTogetherRoomState
import com.mmc.merimusicplayer.data.model.ltw.session.ListenTogetherSessionState

interface PlayerListenTogetherAccess {
    val roomState: StateFlow<ListenTogetherRoomState?>
    val sessionState: StateFlow<ListenTogetherSessionState>
    fun resumeListenerAfterSafetyPause()
    fun isControllerAudioLinkUnavailable(roomId: String, stableKey: String): Boolean
}
