@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.mmc.merimusicplayer.core.player.lan

import androidx.media3.common.Format
import com.mmc.merimusicplayer.core.player.url.buildListenTogetherFallbackAudioInfo
import com.mmc.merimusicplayer.data.model.playback.PlaybackAudioInfo
import com.mmc.merimusicplayer.data.model.playback.PlaybackAudioSource
import com.mmc.merimusicplayer.data.model.playback.deriveCodecLabel

/** Source is authoritative room metadata; quality comes from the selected host candidate. */
internal fun lanAudioSource(channelId: String): PlaybackAudioSource = when (channelId) {
    "local" -> PlaybackAudioSource.LOCAL
    "netease" -> PlaybackAudioSource.NETEASE
    "bilibili" -> PlaybackAudioSource.BILIBILI
    "youtubeMusic" -> PlaybackAudioSource.YOUTUBE_MUSIC
    "subsonic" -> PlaybackAudioSource.SUBSONIC
    else -> error("SOURCE")
}

internal fun lanStreamingAudioMetadata(
    channelId: String, qualityKey: String?, format: Format, localized: (Int) -> String,
): PlaybackAudioInfo {
    val source = lanAudioSource(channelId)
    val base = qualityKey?.let { buildListenTogetherFallbackAudioInfo(source, it, localized) }
        ?: PlaybackAudioInfo(source)
    return base.copy(
        qualityOptions = emptyList(),
        codecLabel = deriveCodecLabel(format.sampleMimeType),
        mimeType = format.sampleMimeType,
        bitrateKbps = format.bitrate.takeIf { it > 0 }?.div(1_000),
        sampleRateHz = format.sampleRate.takeIf { it > 0 },
        channelCount = format.channelCount.takeIf { it > 0 },
    )
}
