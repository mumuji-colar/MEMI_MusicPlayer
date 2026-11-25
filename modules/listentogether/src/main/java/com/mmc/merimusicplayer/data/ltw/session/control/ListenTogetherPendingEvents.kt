package com.mmc.merimusicplayer.data.ltw.session.control

import com.mmc.merimusicplayer.data.model.ltw.session.PendingMemberControlRequest
import com.mmc.merimusicplayer.data.model.ltw.message.event.ListenTogetherCause

fun PendingMemberControlRequest.retriedAt(
    nowElapsedMs: Long
): PendingMemberControlRequest {
    return copy(
        lastSentAtElapsedMs = nowElapsedMs,
        attempts = attempts + 1
    )
}

internal fun PendingMemberControlRequest?.acknowledgedBy(
    cause: ListenTogetherCause?
): PendingMemberControlRequest? {
    val eventId = cause?.eventId?.takeIf { it.isNotBlank() } ?: return this
    return takeUnless { it?.event?.eventId == eventId }
}
