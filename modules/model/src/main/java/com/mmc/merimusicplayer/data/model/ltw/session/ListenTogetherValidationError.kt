package com.mmc.merimusicplayer.data.model.ltw.session

import androidx.annotation.StringRes

data class ListenTogetherValidationError(
    @param:StringRes val messageResId: Int,
    val args: List<Any> = emptyList()
)
