package com.mmc.merimusicplayer.data.ltw.session.membership

import com.mmc.merimusicplayer.data.model.ltw.room.ListenTogetherRoomState
import com.mmc.merimusicplayer.data.ltw.session.normalizedListenTogetherIdentity

fun resolveListenTogetherSessionRole(
    sessionUserId: String?,
    fallbackRole: String?,
    state: ListenTogetherRoomState?
): String? {
    val normalizedUserId = sessionUserId.normalizedListenTogetherIdentity()
    val controllerUserId = state.controllerIdentity()
    return when {
        normalizedUserId != null && controllerUserId != null -> {
            if (normalizedUserId == controllerUserId) "controller" else "listener"
        }

        else -> fallbackRole
    }
}

private fun ListenTogetherRoomState?.controllerIdentity(): String? {
    val room = this ?: return null
    return room.controllerUserUuid.normalizedListenTogetherIdentity()
        ?: room.controllerUserId.normalizedListenTogetherIdentity()
}
