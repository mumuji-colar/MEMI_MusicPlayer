package com.mmc.merimusicplayer.data.sync.schedule

fun shouldDeferAutomaticSyncForPlayback(
    forceSync: Boolean,
    triggerByUserAction: Boolean,
    playbackIntentActive: Boolean
): Boolean {
    return !forceSync && !triggerByUserAction && playbackIntentActive
}
