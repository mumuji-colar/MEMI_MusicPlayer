package com.mmc.merimusicplayer.data.ltw.session.membership

import com.mmc.merimusicplayer.data.model.ltw.room.ListenTogetherRoomState

internal fun shouldApplyListenTogetherClosedRoomPause(
    state: ListenTogetherRoomState
): Boolean {
    return state.playback.state == "paused"
}
