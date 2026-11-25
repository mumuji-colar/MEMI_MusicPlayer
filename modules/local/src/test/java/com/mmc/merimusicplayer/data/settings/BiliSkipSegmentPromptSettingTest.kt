package com.mmc.merimusicplayer.data.settings

import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.data.settings.generated.AutoSettingsMetadata
import com.mmc.merimusicplayer.data.settings.generated.AutoSettingsSections
import com.mmc.merimusicplayer.ksp.annotations.AutoSettingIcon
import com.mmc.merimusicplayer.ksp.annotations.SettingUiType
import com.mmc.merimusicplayer.ksp.annotations.SettingValueType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BiliSkipSegmentPromptSettingTest {
    @Test
    fun `bili skip segment prompt switch is opt in and belongs to general`() {
        val setting = AutoSettingsSchema.general.biliSkipSegmentPromptEnabled
        val metadata = AutoSettingsMetadata.setting("bili_skip_segment_prompt_enabled")

        assertEquals("bili_skip_segment_prompt_enabled", setting.key)
        assertFalse(setting.defaultValue)
        assertEquals(SettingValueType.Boolean, metadata?.valueType)
        assertEquals(SettingUiType.Switch, metadata?.ui)
        assertEquals(AutoSettingsSections.general, metadata?.section)
        assertEquals(CoreCommonR.string.settings_bili_skip_segment_prompt, metadata?.titleRes)
        assertEquals(AutoSettingIcon.Info, setting.icon)
        assertEquals(
            AutoSettingIcon.Storage,
            AutoSettingsSchema.general.alwaysRecordLogsEnabled.icon
        )
        assertNotEquals(AutoSettingsSchema.general.alwaysRecordLogsEnabled.icon, setting.icon)
    }
}
