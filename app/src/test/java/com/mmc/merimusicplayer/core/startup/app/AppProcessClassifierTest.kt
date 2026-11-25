package com.mmc.merimusicplayer.core.startup.app

import android.os.Build
import com.mmc.merimusicplayer.shouldTrimUidtPendingJobs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppProcessClassifierTest {
    @Test
    fun `matches configured main process`() {
        assertTrue(
            AppProcessClassifier.isMainProcess(
                currentProcessName = "com.mmc.merimusicplayer",
                configuredMainProcessName = "com.mmc.merimusicplayer",
                packageName = "com.mmc.merimusicplayer"
            )
        )
    }

    @Test
    fun `falls back to package name when configured process is blank`() {
        assertTrue(
            AppProcessClassifier.isMainProcess(
                currentProcessName = "com.mmc.merimusicplayer",
                configuredMainProcessName = "",
                packageName = "com.mmc.merimusicplayer"
            )
        )
    }

    @Test
    fun `detects secondary process`() {
        assertFalse(
            AppProcessClassifier.isMainProcess(
                currentProcessName = "com.mmc.merimusicplayer:web_login",
                configuredMainProcessName = "com.mmc.merimusicplayer",
                packageName = "com.mmc.merimusicplayer"
            )
        )
    }

    @Test
    fun `trims UIDT jobs only in the main process on supported Android`() {
        assertTrue(
            shouldTrimUidtPendingJobs(
                runningInMainProcess = true,
                sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            )
        )
        assertFalse(
            shouldTrimUidtPendingJobs(
                runningInMainProcess = false,
                sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            )
        )
        assertFalse(
            shouldTrimUidtPendingJobs(
                runningInMainProcess = true,
                sdkInt = Build.VERSION_CODES.TIRAMISU
            )
        )
    }
}
