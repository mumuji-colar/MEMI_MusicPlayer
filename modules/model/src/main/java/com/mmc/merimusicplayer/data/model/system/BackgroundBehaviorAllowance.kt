package com.mmc.merimusicplayer.data.model.system

data class BackgroundBehaviorAllowance(
    val ignoringBatteryOptimizations: Boolean,
    val backgroundAppOpsAllowed: Boolean
) {
    val fullyAllowed: Boolean
        get() = ignoringBatteryOptimizations && backgroundAppOpsAllowed
}
