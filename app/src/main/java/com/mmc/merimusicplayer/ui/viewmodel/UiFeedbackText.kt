package com.mmc.merimusicplayer.ui.viewmodel

import androidx.annotation.StringRes
import com.mmc.merimusicplayer.ui.i18n.UiText

internal fun feedbackText(@StringRes id: Int, vararg arguments: Any?): UiText =
    UiText.Resource(id, arguments.toList())
