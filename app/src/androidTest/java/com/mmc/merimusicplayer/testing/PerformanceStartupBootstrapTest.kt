package com.mmc.merimusicplayer.testing

import android.os.Bundle
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import com.mmc.merimusicplayer.R
import com.mmc.merimusicplayer.activity.MainActivity
import com.mmc.merimusicplayer.common.locale.LanguageManager
import com.mmc.merimusicplayer.core.startup.debug.DebugBuildWarningRepository
import com.mmc.merimusicplayer.data.settings.SettingsRepository
import com.mmc.merimusicplayer.testutil.grantRuntimePermissions
import com.mmc.merimusicplayer.testutil.playbackRuntimePermissions
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.mmc.merimusicplayer.R as AppR

@RunWith(AndroidJUnit4::class)
@ManualInstrumentedTest
class PerformanceStartupBootstrapTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun prepareMainScreenForPerformanceMeasurements() {
        assumeTrue(
            "性能工具需要显式启用启动 fixture",
            InstrumentationRegistry.getArguments().getString("preparePerformanceStartup") == "true"
        )
        grantRuntimePermissions(*playbackRuntimePermissions())
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val settings = SettingsRepository(context)
        val warning = DebugBuildWarningRepository(context)
        // 性能工具确认启动状态，供 instrumentation 结束后的正常进程继续使用
        runBlocking {
            withTimeout(STARTUP_TIMEOUT_MS) {
                settings.setDisclaimerAccepted(true)
                settings.setStartupOnboardingCompleted(true)
                warning.acknowledge()
                assertTrue(settings.disclaimerAcceptedFlow.filterNotNull().first())
                assertTrue(settings.startupOnboardingCompletedFlow.filterNotNull().first())
                assertTrue(warning.isAcknowledged())
            }
        }
        val localizedContext = LanguageManager.localizedContext(
            context,
            LanguageManager.getCurrentLanguage(context)
        )
        val mainTabLabel = localizedContext.getString(AppR.string.nav_explore)
        val startupTitles = listOf(
            localizedContext.getString(AppR.string.disclaimer_title),
            localizedContext.getString(AppR.string.onboarding_title),
            localizedContext.getString(R.string.debug_build_warning_title)
        )
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitUntil(timeoutMillis = STARTUP_TIMEOUT_MS) {
                composeRule.onAllNodesWithContentDescription(mainTabLabel)
                    .fetchSemanticsNodes().isNotEmpty() &&
                    startupTitles.all { title ->
                        composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isEmpty()
                    }
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithContentDescription(mainTabLabel).assertIsDisplayed()
            startupTitles.forEach { title ->
                composeRule.onAllNodesWithText(title).assertCountEquals(0)
            }
        }
        instrumentation.sendStatus(0, Bundle().apply {
            putString("performance_startup_ready", context.packageName)
        })
    }

    private companion object {
        const val STARTUP_TIMEOUT_MS = 20_000L
    }
}
