package com.mmc.merimusicplayer.ui

import android.content.Context
import com.mmc.merimusicplayer.ui.debug.AppDebugCrashActionOwner
import com.mmc.merimusicplayer.ui.debug.appDebugCrashActionOwner
import com.mmc.merimusicplayer.ui.screen.debug.DebugCrashTestType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class AppDebugCrashActionOwnerTest {
    @Test
    fun productionActionsCoverEveryCrashType() {
        val owner = appDebugCrashActionOwner(mock(Context::class.java)) { "test crash" }

        assertEquals(DebugCrashTestType.entries.toSet(), owner.supportedTypes)
    }

    @Test
    fun dispatchInvokesOnlySelectedCrashAction() {
        val invoked = mutableListOf<DebugCrashTestType>()
        val owner = AppDebugCrashActionOwner(
            crashMessage = { "test crash" },
            actions = DebugCrashTestType.entries.associateWith { type ->
                { message ->
                    assertEquals("test crash", message)
                    invoked += type
                }
            }
        )

        DebugCrashTestType.entries.forEach { type ->
            owner.dispatch(type)
        }

        assertEquals(DebugCrashTestType.entries, invoked)
    }
}
