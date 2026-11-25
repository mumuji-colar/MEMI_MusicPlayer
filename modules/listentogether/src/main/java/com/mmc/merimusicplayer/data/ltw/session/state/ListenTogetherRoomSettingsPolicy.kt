package com.mmc.merimusicplayer.data.ltw.session.state

import com.mmc.merimusicplayer.data.model.ltw.room.ListenTogetherRoomSettings

fun ListenTogetherRoomSettings?.normalized(): ListenTogetherRoomSettings {
    return this ?: ListenTogetherRoomSettings()
}
