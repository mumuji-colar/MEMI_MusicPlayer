package com.mmc.merimusicplayer.data.ltw.control

import com.mmc.merimusicplayer.data.model.ltw.session.ListenTogetherPlaybackCommandSnapshot

internal fun <T> resolveListenTogetherPlaybackCommandSnapshot(
    commandQueue: List<T>?,
    commandPositionMs: Long?,
    currentQueue: List<T>,
    currentPositionMs: Long
): ListenTogetherPlaybackCommandSnapshot<T> {
    return ListenTogetherPlaybackCommandSnapshot(
        queue = commandQueue?.takeIf { it.isNotEmpty() } ?: currentQueue,
        positionMs = (commandPositionMs ?: currentPositionMs).coerceAtLeast(0L)
    )
}
