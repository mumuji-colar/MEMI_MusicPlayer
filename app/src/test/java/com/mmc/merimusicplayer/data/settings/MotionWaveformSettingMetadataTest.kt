package com.mmc.merimusicplayer.data.settings

import com.mmc.merimusicplayer.data.settings.generated.AutoSettingsKeys
import com.mmc.merimusicplayer.data.settings.generated.AutoSettingsMetadata
import com.mmc.merimusicplayer.ksp.annotations.SettingUiType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionWaveformSettingMetadataTest {
    @Test
    fun waveformSwitchIsAUserFacingItemInTheMotionSection() {
        val setting = AutoSettingsMetadata.requireSetting(
            AutoSettingsKeys.NOWPLAYING_WAVEFORM_MOTION_ENABLED
        )

        assertEquals("motion", setting.section)
        assertEquals(SettingUiType.Switch, setting.ui)
        assertEquals("nowplaying_waveform_motion_enabled", setting.keyName)
        assertTrue("the switch needs a visible title", setting.titleRes != 0)
        assertTrue("the switch needs a description", setting.descriptionRes != 0)
    }
}
