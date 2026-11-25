package com.mmc.merimusicplayer.testing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mmc.merimusicplayer.core.player.download.AudioDownloadManager
import com.mmc.merimusicplayer.core.startup.app.InstrumentationTestRuntime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstrumentationTestRuntimeTest {

    @Test
    fun runnerMarksInstrumentationRuntimeBeforeApplicationStartup() {
        assertTrue(InstrumentationTestRuntime.isActive)
    }

    @Test
    fun networkRecoveryCallbackDoesNotRunAgainstSharedSafFixtures() {
        AudioDownloadManager.initialize(InstrumentationRegistry.getInstrumentation().targetContext)
        assertFalse(AudioDownloadManager.networkRecoveryMonitorRegistered)
    }
}
