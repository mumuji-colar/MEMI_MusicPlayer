package com.mmc.merimusicplayer.ui.screen

import com.mmc.merimusicplayer.data.model.download.DownloadProgress

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.data.model.download.DownloadStatus
import com.mmc.merimusicplayer.data.model.download.DownloadTask
import com.mmc.merimusicplayer.core.player.download.AudioDownloadManager
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.testutil.assumeComposeHostAvailable
import java.util.UUID
import kotlinx.coroutines.runBlocking
import com.mmc.merimusicplayer.core.download.GlobalDownloadManager
import com.mmc.merimusicplayer.data.model.download.DownloadExecutionRequest
import com.mmc.merimusicplayer.core.download.execution.persistence.DownloadExecutionRoomStore
import com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase
import com.mmc.merimusicplayer.ui.screen.download.DownloadProgressScreen
import com.mmc.merimusicplayer.ui.screen.nowplaying.actions.DownloadProgressContent
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.mmc.merimusicplayer.R as AppR

@RunWith(AndroidJUnit4::class)
class DownloadProgressContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun assumeDeviceUnlocked() {
        assumeComposeHostAvailable()
    }

    @Test
    fun lastDurableTaskDisappearsAndReplacementReappearsWithoutTaskCards() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = MemiUserDataDatabase.getInstance(context)
        val request = DownloadExecutionRequest(
            operationId = "progress-ui-${UUID.randomUUID()}",
            song = SongItem(id = 9_823L, name = "Pending UI fixture", artist = "fixture",
                album = "Netease", albumId = 1L, durationMs = 1_000L, coverUrl = null),
            attemptId = 1L
        )
        val replacement = request.copy(operationId = "${request.operationId}-replacement", attemptId = 2L)
        val pendingText = context.resources.getQuantityString(AppR.plurals.download_tasks_count, 1, 1)
        val emptyText = context.getString(CoreCommonR.string.download_no_tasks)
        fun awaitText(text: String) {
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }
        }
        try {
            assertTrue(GlobalDownloadManager.downloadTasks.value.isEmpty())
            runBlocking {
                DownloadExecutionRoomStore.upsert(context, request, "ASSETS_ENRICHING", database = database)
            }
            composeRule.setContent {
                MaterialTheme {
                    DownloadProgressScreen(
                        onBack = {},
                        listState = rememberLazyListState()
                    )
                }
            }
            awaitText(pendingText)
            runBlocking {
                DownloadExecutionRoomStore.updateState(context, request.operationId, "FINALIZED", database = database)
            }
            awaitText(emptyText)
            composeRule.onNodeWithText(pendingText).assertDoesNotExist()
            runBlocking {
                DownloadExecutionRoomStore.upsert(context, replacement, "QUEUED", database = database)
            }
            awaitText(pendingText)
            assertTrue(GlobalDownloadManager.downloadTasks.value.isEmpty())
        } finally {
            runBlocking {
                database.downloadOperationDao().deleteOperations(listOf(request.operationId, replacement.operationId))
            }
        }
    }

    @Test
    fun waitingNetworkTaskKeepsItsNetworkStatusWhenProgressIsRetained() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val task = DownloadTask(
            song = SongItem(
                id = 1L,
                name = "Test song",
                artist = "Test artist",
                album = "Test album",
                albumId = 1L,
                durationMs = 1_000L,
                coverUrl = null
            ),
            progress = DownloadProgress(
                songKey = "test-song",
                songId = 1L,
                fileName = "test-song.mp3",
                bytesRead = 2L * 1024L * 1024L,
                totalBytes = 8L * 1024L * 1024L,
                speedBytesPerSec = 1024L * 1024L
            ),
            status = DownloadStatus.WAITING_NETWORK,
            attemptId = 1L
        )

        composeRule.setContent {
            MaterialTheme {
                DownloadProgressContent(task)
            }
        }

        composeRule.onNodeWithText(
            context.getString(CoreCommonR.string.download_waiting_network_recovery)
        ).assertExists()
    }
}
