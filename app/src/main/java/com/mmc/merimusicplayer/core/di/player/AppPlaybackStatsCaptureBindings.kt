package com.mmc.merimusicplayer.core.di.player

import android.content.Context
import com.mmc.merimusicplayer.core.player.persistence.stats.flushPlaybackStatsPendingWrites
import com.mmc.merimusicplayer.core.player.persistence.stats.withPlaybackStatsRestore
import com.mmc.merimusicplayer.data.stats.PlaybackStatsCaptureBarrier

internal fun installPlaybackStatsCaptureBarrier(
    runningInMainProcess: Boolean,
    restore: suspend (Context, suspend () -> Unit) -> Unit = ::withPlaybackStatsRestore,
    flush: suspend (Context) -> Unit = ::flushPlaybackStatsPendingWrites
) {
    if (!runningInMainProcess) return
    PlaybackStatsCaptureBarrier.install(
        flush = { context -> flush(context.applicationContext) },
        restore = { context, block -> restore(context.applicationContext, block) }
    )
}
