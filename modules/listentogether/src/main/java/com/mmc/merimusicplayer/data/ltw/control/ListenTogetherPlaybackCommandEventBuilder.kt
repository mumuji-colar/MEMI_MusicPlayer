package com.mmc.merimusicplayer.data.ltw.control

import com.mmc.merimusicplayer.data.ltw.compat.resolveListenTogetherPlaybackCommandShouldPlay
import com.mmc.merimusicplayer.data.ltw.mapping.ListenTogetherSongMapper
import com.mmc.merimusicplayer.data.ltw.playback.ListenTogetherPlaybackHost
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.ltw.message.event.ListenTogetherEvent
import com.mmc.merimusicplayer.data.model.ltw.room.ListenTogetherRoomState
import com.mmc.merimusicplayer.data.model.playback.PlaybackCommand

private data class PlaybackEventSnapshot(val queue: List<SongItem>, val index: Int, val positionMs: Long, val shouldPlay: Boolean)

internal class ListenTogetherPlaybackCommandEventBuilder(
    private val factory: ListenTogetherEventFactory,
    private val playback: ListenTogetherPlaybackHost,
    songMapper: ListenTogetherSongMapper,
    private val roomState: () -> ListenTogetherRoomState?,
    private val isController: () -> Boolean,
    private val transportActive: () -> Boolean
) : ListenTogetherSongMapper by songMapper {
    fun build(command: PlaybackCommand): ListenTogetherEvent? {
        val snapshot = snapshot(command)
        if (command.type in TRACK_SELECTION_COMMANDS) return trackSelection(snapshot)
        return when (command.type) {
            "PLAY", "PAUSE" -> transport(command.type, snapshot.positionMs)
            "SEEK" -> seek(snapshot)
            "PLAYBACK_MODE" -> playbackMode(command)
            "SET_QUEUE" -> factory.buildSetQueueEvent(snapshot.queue, snapshot.index, snapshot.positionMs, command.shouldPlay)
            "TRACK_FINISHED" -> factory.buildTrackFinishedEvent(command, snapshot.queue, playback.currentSongFlow.value, snapshot.positionMs)
            else -> null
        }
    }

    private fun playbackMode(command: PlaybackCommand): ListenTogetherEvent =
        factory.buildPlaybackModeEvent(command.repeatMode ?: playback.repeatModeFlow.value, command.shuffleEnabled ?: playback.shuffleModeFlow.value)

    private fun snapshot(command: PlaybackCommand): PlaybackEventSnapshot {
        val captured = resolveListenTogetherPlaybackCommandSnapshot(command.queue, command.positionMs, playback.currentQueueFlow.value, playback.playbackPositionFlow.value)
        val queue = queueFor(command, captured.queue)
        return PlaybackEventSnapshot(
            queue, command.currentIndex ?: queue.indexOfTrack(playback.currentSongFlow.value).coerceAtLeast(0), captured.positionMs,
            resolveListenTogetherPlaybackCommandShouldPlay(command.type, command.shouldPlay, transportActive(), playback.isPlayingFlow.value)
        )
    }

    private fun queueFor(command: PlaybackCommand, captured: List<SongItem>): List<SongItem> {
        if (command.type == "SET_QUEUE") return command.queue ?: captured
        return captured
    }

    private fun trackSelection(snapshot: PlaybackEventSnapshot): ListenTogetherEvent? {
        if (!snapshot.queue.hasShareableListenTogetherTrackAt(snapshot.index)) return null
        val event = factory.buildSetTrackEvent(snapshot.queue, snapshot.index, snapshot.positionMs, snapshot.shouldPlay)
        return if (isController()) event else event.copy(type = "REQUEST_SET_TRACK")
    }

    private fun transport(type: String, position: Long): ListenTogetherEvent? {
        if (!playback.currentSongFlow.value.isShareableForListenTogether()) return null
        val event = if (type == "PLAY") factory.buildPlayEvent(position) else factory.buildPauseEvent(position)
        return if (isController()) event else event.copy(type = "REQUEST_$type")
    }

    private fun seek(snapshot: PlaybackEventSnapshot): ListenTogetherEvent? {
        if (!playback.currentSongFlow.value.isShareableForListenTogether()) return null
        if (!snapshot.queue.hasShareableListenTogetherTrackAt(snapshot.index)) return null
        val (tracks, index) = snapshot.queue.toShareableQueueSnapshot(snapshot.index, roomState()?.settings, includeResolvedStreamUrl = false)
        val event = if (isController()) factory.buildSeekEvent(snapshot.positionMs) else factory.buildRequestSeekEvent(snapshot.positionMs)
        return event.copy(currentIndex = index, track = tracks.getOrNull(index))
    }

    private companion object {
        val TRACK_SELECTION_COMMANDS = setOf("PLAY_PLAYLIST", "PLAY_FROM_QUEUE", "NEXT", "PREVIOUS")
    }
}
