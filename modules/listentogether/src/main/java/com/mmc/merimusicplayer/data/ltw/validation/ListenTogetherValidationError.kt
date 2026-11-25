package com.mmc.merimusicplayer.data.ltw.validation

import com.mmc.merimusicplayer.data.model.ltw.session.ListenTogetherValidationError

import android.content.Context
import com.mmc.merimusicplayer.common.locale.LanguageManager

fun ListenTogetherValidationError.format(context: Context): String {
    val localizedContext = LanguageManager.applyLanguage(context.applicationContext)
    return localizedContext.getString(messageResId, *args.toTypedArray())
}
