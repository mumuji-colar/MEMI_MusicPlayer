package com.mmc.merimusicplayer.data.ltw.session.membership

import com.mmc.merimusicplayer.data.model.ltw.session.ListenTogetherMembershipCredential
import com.mmc.merimusicplayer.data.model.ltw.session.ListenTogetherSessionState
import com.mmc.merimusicplayer.data.ltw.session.normalizedListenTogetherIdentity

fun ListenTogetherSessionState.toMembershipCredentialOrNull():
    ListenTogetherMembershipCredential? {
    val normalizedBaseUrl = baseUrl.normalizedListenTogetherIdentity() ?: return null
    val normalizedRoomId = roomId.normalizedListenTogetherIdentity() ?: return null
    val normalizedUserUuid = userUuid.normalizedListenTogetherIdentity() ?: return null
    val normalizedToken = token.normalizedListenTogetherIdentity()
    val normalizedMemberSecret = memberSecret.normalizedListenTogetherIdentity()
    if (normalizedToken == null && normalizedMemberSecret == null) {
        return null
    }
    return ListenTogetherMembershipCredential(
        baseUrl = normalizedBaseUrl,
        roomId = normalizedRoomId,
        userUuid = normalizedUserUuid,
        token = normalizedToken,
        memberSecret = normalizedMemberSecret,
        joinSecret = joinSecret.normalizedListenTogetherIdentity()
    )
}

fun resolveReusableListenTogetherMembershipCredential(
    activeSession: ListenTogetherSessionState,
    retainedCredential: ListenTogetherMembershipCredential?,
    baseUrl: String,
    roomId: String,
    userUuid: String
): ListenTogetherMembershipCredential? {
    return sequenceOf(activeSession.toMembershipCredentialOrNull(), retainedCredential)
        .filterNotNull()
        .firstOrNull { credential ->
            credential.baseUrl.equals(baseUrl.trim(), ignoreCase = true) &&
                credential.roomId.equals(roomId.trim(), ignoreCase = true) &&
                credential.userUuid.equals(userUuid.trim(), ignoreCase = true)
        }
}
