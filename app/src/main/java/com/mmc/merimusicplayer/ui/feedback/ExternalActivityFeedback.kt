package com.mmc.merimusicplayer.ui.feedback

import android.content.Context
import android.content.Intent
import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.util.platform.tryStartActivity

/** 启动外部页面失败时给出统一提示，而不是让当前页面因 ActivityNotFoundException 崩溃 */
fun Context.startActivityOrShowUnavailable(intent: Intent): Boolean {
    val started = tryStartActivity(intent)
    if (!started) {
        AppFeedback.show(this, getString(CoreCommonR.string.error_no_app_for_action))
    }
    return started
}
