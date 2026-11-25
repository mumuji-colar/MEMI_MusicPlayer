package com.mmc.merimusicplayer.data.ltw.lifecycle

import kotlinx.coroutines.Job

internal fun cancelListenTogetherBackgroundJobs(vararg jobs: Job?) {
    jobs.forEach { it?.cancel() }
}
