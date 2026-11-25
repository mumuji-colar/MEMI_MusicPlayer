package com.mmc.merimusicplayer.data.local.database.store


import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import com.mmc.merimusicplayer.data.history.toSongItem
import com.mmc.merimusicplayer.data.model.history.PlayedEntry
import com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayHistoryRoomStoreTest {
    @Test
    fun incrementalWriteUpdatesOnlyChangedEntries() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(
            context,
            MemiUserDataDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            val first = PlayedEntry(
                id = 1L,
                name = "first",
                artist = "artist",
                album = "netease",
                durationMs = 180_000L,
                coverUrl = null,
                mediaUri = "content://media/external/audio/media/1",
                explicitLocalSourceReference = "content://media/external/audio/media/1",
                playedAt = 100L
            )
            val second = first.copy(
                id = 2L,
                name = "second",
                mediaUri = null,
                explicitLocalSourceReference = null,
                playedAt = 50L
            )
            val store = PlayHistoryRoomStore(database)
            store.importLegacyAndPromote(listOf(first, second))

            val updatedFirst = first.copy(
                resumePositionMs = 4_000L,
                playedAt = 200L
            )
            store.writeIncremental(
                previous = listOf(first, second),
                next = listOf(updatedFirst)
            )

            assertEquals(listOf(updatedFirst), store.readIfRoomPrimary())
            assertEquals(updatedFirst.explicitLocalSourceReference,
                store.readIfRoomPrimary()?.single()?.toSongItem()?.explicitLocalSourceReference)
            assertEquals(1, database.playHistoryDao().getAll().size)
            assertTrue(
                database.syncMetadataDao().getMigrationMetadata(
                    PlayHistoryRoomStore.CUTOVER_STATE_METADATA_KEY
                ) != null
            )
        } finally {
            database.close()
        }
    }
}
