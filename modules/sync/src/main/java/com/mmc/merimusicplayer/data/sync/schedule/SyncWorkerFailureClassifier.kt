package com.mmc.merimusicplayer.data.sync.schedule

import com.mmc.merimusicplayer.data.model.sync.SyncWorkerFailureKind

class SyncWorkerFailureClassifier(private val kinds: Map<Class<out Throwable>, SyncWorkerFailureKind>) {
    fun classify(error: Throwable?): SyncWorkerFailureKind = kinds.entries
        .firstOrNull { it.key.isInstance(error) }?.value ?: SyncWorkerFailureKind.OTHER
}
