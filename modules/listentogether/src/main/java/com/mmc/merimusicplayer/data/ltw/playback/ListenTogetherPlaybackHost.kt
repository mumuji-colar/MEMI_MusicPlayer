package com.mmc.merimusicplayer.data.ltw.playback

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import com.mmc.merimusicplayer.data.ltw.session.link.ListenTogetherLinkPlaybackPort
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.playback.PlaybackCommand
import com.mmc.merimusicplayer.data.model.playback.PlaybackCommandSource

interface ListenTogetherPlaybackHost : ListenTogetherLinkPlaybackPort {
    val currentSongFlow: StateFlow<SongItem?>
    val currentQueueFlow: StateFlow<List<SongItem>>
    val isPlayingFlow: StateFlow<Boolean>
    val playWhenReadyFlow: StateFlow<Boolean>
    val playerPlaybackStateFlow: StateFlow<Int>
    val playbackPositionFlow: StateFlow<Long>
    val currentMediaUrlFlow: StateFlow<String?>
    val repeatModeFlow: StateFlow<Int>
    val shuffleModeFlow: StateFlow<Boolean>
    val playbackCommandFlow: Flow<PlaybackCommand>
    val shuffleRestorePlaylistReference: List<SongItem>?
    val listenTogetherSyncPlaybackRate: Float
    fun pauseImpl(forcePersist: Boolean, commandSource: PlaybackCommandSource, allowFadeOut: Boolean, debugReason: String)
    fun play(commandSource: PlaybackCommandSource)
    fun seekTo(positionMs: Long, commandSource: PlaybackCommandSource)
    fun playPlaylist(queue: List<SongItem>, currentIndex: Int, commandSource: PlaybackCommandSource)
    fun applyRemoteQueueUpdate(queue: List<SongItem>, currentIndex: Int)
    fun applyListenTogetherPlaybackMode(repeatMode: Int?, shuffleEnabled: Boolean?)
    fun resetListenTogetherSyncPlaybackRate()
    fun setListenTogetherSyncPlaybackRate(rate: Float)
    fun retryListenTogetherSafetyPauseResume()
    fun completeListenTogetherSafetyPauseResume()
    fun clearListenTogetherSafetyPause()
    fun shouldHoldListenTogetherPlaybackForSafetyPause(causeType: String?): Boolean
    fun shouldWaitForListenTogetherAuthoritativeStream(song: SongItem): Boolean
    fun isListenTogetherLocalResolutionPendingFor(song: SongItem): Boolean
    fun isListenTogetherAuthoritativeStreamConfirmedUnavailable(song: SongItem): Boolean
    fun currentPlaybackRequiresListenTogetherAuthoritativeStream(): Boolean
    fun currentListenTogetherShareableStreamUrls(): List<String>
    fun isPendingMediaLoadActive(): Boolean
    fun isTransportActive(): Boolean
    fun isUsbExclusiveOutputEnabled(): Boolean
}
