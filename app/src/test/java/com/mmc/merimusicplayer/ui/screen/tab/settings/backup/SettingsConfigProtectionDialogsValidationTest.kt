package com.mmc.merimusicplayer.ui.screen.tab.settings.backup

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.data.model.settings.appearance.UiStyle
import com.mmc.merimusicplayer.ui.theme.MemiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import com.mmc.merimusicplayer.R as AppR

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class SettingsConfigProtectionDialogsValidationTest {
    @get:Rule val composeRule = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `clean export rejects a password longer than the encrypted format supports`() =
        verifyPasswordLimit(UiStyle.Clean)

    @Test
    fun `material export rejects a password longer than the encrypted format supports`() =
        verifyPasswordLimit(UiStyle.Material)

    private fun verifyPasswordLimit(style: UiStyle) {
        composeRule.setContent {
            MemiTheme(
                followSystemDark = false,
                forceDark = false,
                dynamicColor = false,
                seedColorHex = "6750A4",
                uiStyle = style,
                animateColors = false
            ) {
                SettingsConfigExportOptionsDialog(onConfirm = { _, _ -> }, onDismiss = {})
            }
        }
        composeRule.onNodeWithText(string(AppR.string.settings_config_encrypt_file))
            .performScrollTo().performClick()

        enterPassword("x".repeat(1024))
        composeRule.onNodeWithText(string(CoreCommonR.string.settings_backup_action_export)).assertIsEnabled()

        enterPassword("x".repeat(1025))
        composeRule.onNodeWithText(string(CoreCommonR.string.settings_backup_action_export)).assertIsNotEnabled()
    }

    private fun enterPassword(password: String) {
        for (id in listOf(AppR.string.settings_config_password, AppR.string.settings_config_confirm_password)) {
            composeRule.onNodeWithText(string(id)).performScrollTo().performTextReplacement(password)
        }
    }

    private fun string(id: Int): String = context.getString(id)
}
