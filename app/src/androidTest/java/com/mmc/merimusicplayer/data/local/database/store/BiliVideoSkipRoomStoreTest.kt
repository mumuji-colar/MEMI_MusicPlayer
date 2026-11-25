package com.mmc.merimusicplayer.data.local.database.store

import com.mmc.merimusicplayer.platform.bilibili.skip.storage.BiliVideoSkipRoomStore

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase
import com.mmc.merimusicplayer.data.model.bilibili.skip.BiliVideoSkipDraft
import com.mmc.merimusicplayer.data.model.bilibili.skip.BiliVideoSkipInterval
import com.mmc.merimusicplayer.data.model.bilibili.skip.BiliVideoSkipRule
import com.mmc.merimusicplayer.data.model.bilibili.skip.BiliVideoSkipTarget
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import com.mmc.merimusicplayer.data.model.bilibili.skip.BiliVideoSkipSnapshot

@RunWith(AndroidJUnit4::class)
class BiliVideoSkipRoomStoreTest {
    @Test
    fun rulesAndDraftsRoundTripWithoutJsonPayloads() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(
            context,
            MemiUserDataDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            val target = BiliVideoSkipTarget("BV1TEST", 12L)
            val rule = BiliVideoSkipRule(
                target = target,
                intervals = listOf(
                    BiliVideoSkipInterval(startMs = 1_000L, endMs = 2_000L),
                    BiliVideoSkipInterval(startMs = 3_000L, endMs = 4_000L)
                ),
                modifiedAt = 20L
            )
            val draft = BiliVideoSkipDraft(
                target = target,
                startText = "00:01",
                endText = "00:02",
                modifiedAt = 21L
            )
            val store = BiliVideoSkipRoomStore(database)

            store.replaceAll(listOf(rule), listOf(draft), now = 30L)

            assertEquals(
                BiliVideoSkipSnapshot(listOf(rule), listOf(draft)),
                store.readIfPrimary()
            )
            assertEquals(1, database.biliVideoSkipDao().getRules().size)
            assertEquals(2, database.biliVideoSkipDao().getIntervals().size)
            assertEquals(1, database.biliVideoSkipDao().getDrafts().size)
        } finally {
            database.close()
        }
    }
}
