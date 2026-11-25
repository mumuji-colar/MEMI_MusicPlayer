package com.mmc.merimusicplayer.data.model.ltw.session

data class ListenTogetherMembershipCredential(
    val baseUrl: String,
    val roomId: String,
    val userUuid: String,
    val token: String?,
    val memberSecret: String?,
    val joinSecret: String?
)
