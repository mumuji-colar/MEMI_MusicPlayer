package com.mmc.merimusicplayer.data.model.ltw.session

import com.mmc.merimusicplayer.data.model.ltw.message.event.ListenTogetherEvent

data class PendingTrackFinishedLegacyFallback(
    val event: ListenTogetherEvent,
    val createdAtElapsedMs: Long,
    val attempted: Boolean = false
)

data class PendingMemberControlRequest(
    val event: ListenTogetherEvent,
    val createdAtElapsedMs: Long,
    val lastSentAtElapsedMs: Long,
    val attempts: Int
)
