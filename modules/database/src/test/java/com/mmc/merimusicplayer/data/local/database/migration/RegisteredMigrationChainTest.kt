package com.mmc.merimusicplayer.data.local.database.migration

import com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase
import org.junit.Assert.assertEquals
import org.junit.Test

class RegisteredMigrationChainTest {
    @Test
    fun `registered migrations upgrade every released version to the final schema`() {
        val steps = MemiUserDataDatabase.allMigrations().map { it.startVersion to it.endVersion }

        assertEquals(
            (1 until MemiUserDataDatabase.FINAL_DB_VERSION).map { it to it + 1 },
            steps
        )
    }
}
