package com.mmc.merimusicplayer.data.ltw.validation

import com.mmc.merimusicplayer.data.model.ltw.session.ListenTogetherValidationError

import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.ltw.mapping.ListenTogetherSongMapper

fun validateListenTogetherRoomCreation(
    queue: List<SongItem>,
    currentIndex: Int,
    currentSong: SongItem?,
    songMapper: ListenTogetherSongMapper
): ListenTogetherValidationError? {
    val currentSongValue = currentSong
        ?: return ListenTogetherValidationError(CoreCommonR.string.listen_together_error_current_track_missing)
    val currentTrack = with(songMapper) { currentSongValue.toListenTogetherTrackOrNull() }
        ?: return ListenTogetherValidationError(CoreCommonR.string.listen_together_error_current_track_not_shareable)
    val queueSong = queue.getOrNull(currentIndex)
        ?: return ListenTogetherValidationError(
            CoreCommonR.string.listen_together_error_current_track_unavailable
        )
    val queueTrack = with(songMapper) { queueSong.toListenTogetherTrackOrNull() }
        ?: return ListenTogetherValidationError(
            CoreCommonR.string.listen_together_error_current_track_not_shareable
        )
    return if (queueTrack.stableKey == currentTrack.stableKey) {
        null
    } else {
        ListenTogetherValidationError(CoreCommonR.string.listen_together_error_current_track_unavailable)
    }
}

internal fun requireValidListenTogetherRoomCreation(
    queue: List<SongItem>,
    currentIndex: Int,
    currentSong: SongItem?,
    songMapper: ListenTogetherSongMapper,
    formatValidationError: (ListenTogetherValidationError) -> String
) {
    validateListenTogetherRoomCreation(queue, currentIndex, currentSong, songMapper)
        ?.let { error(formatValidationError(it)) }
}
