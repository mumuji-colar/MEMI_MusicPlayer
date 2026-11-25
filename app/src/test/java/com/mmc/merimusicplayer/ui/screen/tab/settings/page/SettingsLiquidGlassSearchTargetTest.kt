package com.mmc.merimusicplayer.ui.screen.tab.settings.page

import com.mmc.merimusicplayer.data.settings.generated.AutoSettingsMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsLiquidGlassSearchTargetTest {
    @Test
    fun liquidGlassSearchTargetsTheAlwaysVisibleBlurPrerequisite() {
        val setting = AutoSettingsMetadata.settings.single { it.keyName == "liquid_glass_enabled" }

        assertEquals(SettingsPage.Motion, setting.settingsPage())
        assertEquals("setting:advanced_blur_enabled", setting.searchTargetId())
        assertEquals(
            2,
            settingsSearchScrollAnchor(SettingsPage.Motion, setting.searchTargetId()).itemIndex
        )
    }
}
