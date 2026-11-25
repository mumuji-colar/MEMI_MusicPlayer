package com.mmc.merimusicplayer.data.ltw

import com.mmc.merimusicplayer.data.model.ltw.room.ListenTogetherRoomState
import com.mmc.merimusicplayer.data.ltw.session.membership.resolveListenTogetherJoinAutoPauseCause as resolveSessionJoinAutoPauseCause

internal fun resolveListenTogetherJoinAutoPauseCause(
    autoPauseOnJoin: Boolean,
    role: String?,
    state: ListenTogetherRoomState
): String? {
    return resolveSessionJoinAutoPauseCause(
        autoPauseOnJoin = autoPauseOnJoin,
        role = role,
        state = state
    )
}
