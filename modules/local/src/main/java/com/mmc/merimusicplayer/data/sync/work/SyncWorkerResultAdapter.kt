package com.mmc.merimusicplayer.data.sync.work

import androidx.work.ListenableWorker
import com.mmc.merimusicplayer.data.model.sync.SyncWorkerOutcome

internal fun SyncWorkerOutcome.toWorkResult(): ListenableWorker.Result = when (this) {
    SyncWorkerOutcome.SUCCESS -> ListenableWorker.Result.success()
    SyncWorkerOutcome.RETRY -> ListenableWorker.Result.retry()
    SyncWorkerOutcome.FAILURE -> ListenableWorker.Result.failure()
}
