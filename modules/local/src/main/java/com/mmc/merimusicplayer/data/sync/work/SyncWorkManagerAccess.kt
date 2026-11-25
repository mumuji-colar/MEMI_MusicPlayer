package com.mmc.merimusicplayer.data.sync.work

import android.content.Context
import androidx.work.WorkManager

internal fun syncWorkManager(context: Context): WorkManager = WorkManager.getInstance(context)
