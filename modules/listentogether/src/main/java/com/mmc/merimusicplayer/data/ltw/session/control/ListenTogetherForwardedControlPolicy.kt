package com.mmc.merimusicplayer.data.ltw.session.control

fun shouldRejectForwardedListenTogetherMemberControl(
    requesterUuid: String?,
    controllerUserUuid: String?,
    allowMemberControl: Boolean
): Boolean {
    if (allowMemberControl) return false
    val isRequesterController = !requesterUuid.isNullOrBlank() && requesterUuid == controllerUserUuid
    return !isRequesterController
}
