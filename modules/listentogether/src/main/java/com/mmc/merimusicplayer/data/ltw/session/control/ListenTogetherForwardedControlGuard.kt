package com.mmc.merimusicplayer.data.ltw.session.control

import com.mmc.merimusicplayer.data.ltw.compat.isListenTogetherMemberControlTargetCurrent
import com.mmc.merimusicplayer.data.ltw.control.trackBoundRequestControlEventTypes
import com.mmc.merimusicplayer.data.ltw.playback.currentStableKey
import com.mmc.merimusicplayer.data.ltw.playback.requestedStableKey
import com.mmc.merimusicplayer.data.ltw.session.state.normalized
import com.mmc.merimusicplayer.data.model.ltw.message.event.ListenTogetherEvent
import com.mmc.merimusicplayer.data.model.ltw.message.socket.ListenTogetherSocketEnvelope
import com.mmc.merimusicplayer.data.model.ltw.room.ListenTogetherRoomState

internal fun forwardedListenTogetherRejectionReason(room: ListenTogetherRoomState?, message: ListenTogetherSocketEnvelope, event: ListenTogetherEvent): String? {
    if (room == null) return "reject_member_control_room_unknown"
    if (shouldRejectForwardedListenTogetherMemberControl(message.causedBy?.userUuid, room.controllerUserUuid, room.settings.normalized().allowMemberControl)) {
        return "reject_member_control_disabled"
    }
    if (hasStaleTarget(room, message, event)) return "reject_stale_member_control"
    return null
}

private fun hasStaleTarget(room: ListenTogetherRoomState, message: ListenTogetherSocketEnvelope, event: ListenTogetherEvent): Boolean {
    val type = message.causedBy?.type ?: return false
    if (type !in trackBoundRequestControlEventTypes) return false
    return !isListenTogetherMemberControlTargetCurrent(type, event.requestedStableKey(), room.currentStableKey())
}
