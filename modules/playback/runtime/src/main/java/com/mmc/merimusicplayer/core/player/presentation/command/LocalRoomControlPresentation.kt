package com.mmc.merimusicplayer.core.player.presentation.command

import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.core.player.policy.command.LocalRoomControlRestriction

internal val LocalRoomControlRestriction.errorResId: Int?
    get() = when (this) {
        LocalRoomControlRestriction.NONE -> null
        LocalRoomControlRestriction.CONTROLLER_OFFLINE -> CoreCommonR.string.listen_together_error_controller_offline
        LocalRoomControlRestriction.MEMBER_CONTROL_DISABLED -> CoreCommonR.string.listen_together_error_member_control_disabled
    }
