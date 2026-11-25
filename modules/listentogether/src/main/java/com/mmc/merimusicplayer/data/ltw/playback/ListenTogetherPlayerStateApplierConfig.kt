package com.mmc.merimusicplayer.data.ltw.playback

internal data class ListenTogetherPlayerStateApplierConfig(
    val tag: String,
    val trackSwitchForceSyncMs: Long,
    val heartbeatDriftForceSyncMs: Long,
    val playingDriftForceSyncMs: Long,
    val pausedDriftForceSyncMs: Long,
    val softSyncMinDriftMs: Long,
    val softSyncFastDriftMs: Long,
    val trackSwitchGracePeriodMs: Long,
    val zeroPositionRollbackGuardMs: Long
)
