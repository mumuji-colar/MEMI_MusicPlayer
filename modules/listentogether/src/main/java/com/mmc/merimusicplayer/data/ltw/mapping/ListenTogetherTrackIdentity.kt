package com.mmc.merimusicplayer.data.ltw.mapping

import com.mmc.merimusicplayer.data.model.ltw.track.listenTogetherTrackKey

fun buildStableTrackKey(channelId: String, audioId: String, subAudioId: String? = null, playlistContextId: String? = null): String =
    listenTogetherTrackKey(channelId, audioId, subAudioId, playlistContextId)
