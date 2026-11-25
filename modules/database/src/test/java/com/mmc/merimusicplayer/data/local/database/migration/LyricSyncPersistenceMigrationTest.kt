package com.mmc.merimusicplayer.data.local.database.migration

import com.mmc.merimusicplayer.data.local.database.migration.legacy.LegacyMigrationDatabase
import com.mmc.merimusicplayer.data.local.database.migration.library.LyricSyncPersistenceMigration
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricSyncPersistenceMigrationTest {
    @Test
    fun `upgrade preserves all existing lyric columns and adds nullable payloads`() {
        val fixture = LegacyMigrationDatabase()
        LyricSyncPersistenceMigration.migrate(fixture.database)
        assertEquals(listOf(
            "ALTER TABLE `play_history` ADD COLUMN `lyric_sync_payload_json` TEXT",
            "ALTER TABLE `playback_queue_song` ADD COLUMN `lyric_sync_payload_json` TEXT"
        ), fixture.statements)
    }
}
