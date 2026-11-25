package com.mmc.merimusicplayer.data.settings.download

import com.mmc.merimusicplayer.data.model.settings.download.DownloadAudioQualitySelection

import com.mmc.merimusicplayer.data.settings.SettingsKeys
import com.mmc.merimusicplayer.data.settings.AutoSettingsSchema
import com.mmc.merimusicplayer.data.settings.bootstrap.readBootstrapSettingsSnapshotSync
import com.mmc.merimusicplayer.data.settings.bootstrap.updateBootstrapDownloadFollowPlaybackAudioQuality
import com.mmc.merimusicplayer.data.settings.dataStore
import com.mmc.merimusicplayer.data.settings.setAutoSetting
import com.mmc.merimusicplayer.data.settings.valueOf
import android.content.Context
import kotlinx.coroutines.flow.first
import com.mmc.merimusicplayer.common.coroutines.runCatchingNonCancellation

internal const val DEFAULT_DOWNLOAD_NETEASE_AUDIO_QUALITY = "exhigh"
internal const val DEFAULT_DOWNLOAD_YOUTUBE_AUDIO_QUALITY = "high"
internal const val DEFAULT_DOWNLOAD_BILI_AUDIO_QUALITY = "high"

private val DOWNLOAD_NETEASE_AUDIO_QUALITIES = setOf(
    "standard",
    "higher",
    "exhigh",
    "lossless",
    "hires",
    "jyeffect",
    "sky",
    "jymaster"
)

private val DOWNLOAD_YOUTUBE_AUDIO_QUALITIES = setOf(
    "low",
    "medium",
    "high",
    "very_high"
)

private val DOWNLOAD_BILI_AUDIO_QUALITIES = setOf(
    "low",
    "medium",
    "high",
    "lossless",
    "hires",
    "dolby"
)

fun normalizeDownloadNeteaseAudioQuality(value: String?): String {
    val normalized = value.orEmpty().trim().lowercase()
    return normalized.takeIf { it in DOWNLOAD_NETEASE_AUDIO_QUALITIES }
        ?: DEFAULT_DOWNLOAD_NETEASE_AUDIO_QUALITY
}

fun normalizeDownloadYouTubeAudioQuality(value: String?): String {
    val normalized = value.orEmpty().trim().lowercase()
    return normalized.takeIf { it in DOWNLOAD_YOUTUBE_AUDIO_QUALITIES }
        ?: DEFAULT_DOWNLOAD_YOUTUBE_AUDIO_QUALITY
}

fun normalizeDownloadBiliAudioQuality(value: String?): String {
    val normalized = value.orEmpty().trim().lowercase()
    return normalized.takeIf { it in DOWNLOAD_BILI_AUDIO_QUALITIES }
        ?: DEFAULT_DOWNLOAD_BILI_AUDIO_QUALITY
}

fun resolveDownloadAudioQualitySelection(
    followsPlaybackQuality: Boolean,
    playbackNeteaseQuality: String?,
    playbackYouTubeQuality: String?,
    playbackBiliQuality: String?,
    downloadNeteaseQuality: String?,
    downloadYouTubeQuality: String?,
    downloadBiliQuality: String?
): DownloadAudioQualitySelection {
    return if (followsPlaybackQuality) {
        DownloadAudioQualitySelection.normalized(
            neteaseQuality = playbackNeteaseQuality,
            youtubeQuality = playbackYouTubeQuality,
            biliQuality = playbackBiliQuality
        )
    } else {
        DownloadAudioQualitySelection.normalized(
            neteaseQuality = downloadNeteaseQuality,
            youtubeQuality = downloadYouTubeQuality,
            biliQuality = downloadBiliQuality
        )
    }
}

suspend fun resolveDownloadAudioQualitySelection(
    context: Context
): DownloadAudioQualitySelection {
    return runCatchingNonCancellation {
        val preferences = context.applicationContext.dataStore.data.first()
        val followsPlaybackQuality = preferences.valueOf(
            AutoSettingsSchema.download.downloadFollowPlaybackAudioQuality
        )
        resolveDownloadAudioQualitySelection(
            followsPlaybackQuality = followsPlaybackQuality,
            playbackNeteaseQuality = preferences[SettingsKeys.AUDIO_QUALITY],
            playbackYouTubeQuality = preferences[SettingsKeys.YOUTUBE_AUDIO_QUALITY],
            playbackBiliQuality = preferences[SettingsKeys.BILI_AUDIO_QUALITY],
            downloadNeteaseQuality = preferences.valueOf(
                AutoSettingsSchema.download.downloadNeteaseAudioQuality
            ),
            downloadYouTubeQuality = preferences.valueOf(
                AutoSettingsSchema.download.downloadYouTubeAudioQuality
            ),
            downloadBiliQuality = preferences.valueOf(
                AutoSettingsSchema.download.downloadBiliAudioQuality
            )
        )
    }.getOrElse {
        DownloadAudioQualitySelection.normalized(
            neteaseQuality = DEFAULT_DOWNLOAD_NETEASE_AUDIO_QUALITY,
            youtubeQuality = DEFAULT_DOWNLOAD_YOUTUBE_AUDIO_QUALITY,
            biliQuality = DEFAULT_DOWNLOAD_BILI_AUDIO_QUALITY
        )
    }
}

fun readDownloadFollowPlaybackAudioQualityStartupValue(context: Context): Boolean {
    return readBootstrapSettingsSnapshotSync(context)
        .downloadFollowPlaybackAudioQuality
}

suspend fun setDownloadFollowPlaybackAudioQuality(
    context: Context,
    followsPlaybackQuality: Boolean
) {
    val appContext = context.applicationContext
    appContext.setAutoSetting(
        AutoSettingsSchema.download.downloadFollowPlaybackAudioQuality,
        followsPlaybackQuality
    )
    updateDownloadFollowPlaybackAudioQualityStartupValue(appContext, followsPlaybackQuality)
}

fun updateDownloadFollowPlaybackAudioQualityStartupValue(
    context: Context,
    followsPlaybackQuality: Boolean
) {
    updateBootstrapDownloadFollowPlaybackAudioQuality(
        context = context,
        followsPlaybackQuality = followsPlaybackQuality
    )
}

fun DownloadAudioQualitySelection.Companion.normalized(
    neteaseQuality: String?,
    youtubeQuality: String?,
    biliQuality: String?
): DownloadAudioQualitySelection {
    return DownloadAudioQualitySelection(
        neteaseQuality = normalizeDownloadNeteaseAudioQuality(neteaseQuality),
        youtubeQuality = normalizeDownloadYouTubeAudioQuality(youtubeQuality),
        biliQuality = normalizeDownloadBiliAudioQuality(biliQuality)
    )
}
