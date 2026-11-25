package com.mmc.merimusicplayer.core.download.resource

import com.mmc.merimusicplayer.data.identity.stableKey

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import com.mmc.merimusicplayer.core.download.GlobalDownloadManager
import com.mmc.merimusicplayer.core.player.download.AudioDownloadManager
import com.mmc.merimusicplayer.core.player.download.source.RetryableDownloadFailureException
import com.mmc.merimusicplayer.core.player.download.runtime.handleDownloadAttemptFailure
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.stableKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadStorageSpaceGuardInstrumentedTest {
    @Test
    fun temporarySpaceFailuresReleaseExecutionAfterBoundedRetriesAndPreservePartialFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val track = SongItem(88330001L, "storage-retry", "test", "netease", 0L, 1000L, null)
        val operationId = "storage-retry-${UUID.randomUUID()}"
        val attemptId = GlobalDownloadManager.taskStore.ensureDownloadTasks(listOf(track))
            .getValue(track.stableKey())
        val partial = File.createTempFile("storage-retry", ".download", context.cacheDir)
        val bytes = byteArrayOf(1, 2, 3, 4)
        partial.writeBytes(bytes)
        AudioDownloadManager.operationRegistry.beginSongDownloadOperation(track.stableKey(), operationId, attemptId)
        try {
            for (knownSpace in listOf(false, true)) {
                val state = AudioDownloadManager.DownloadExecutionAttemptState(
                    tempFile = partial, attemptNumber = AudioDownloadManager.TRANSIENT_DOWNLOAD_MAX_ATTEMPTS - 1
                )
                val failure = DownloadStorageSpaceException(
                    rootPath = context.cacheDir.path, usableBytes = 100L, reservedBytes = 100L,
                    requestedBytes = 20L, minimumFreeBytes = 1L, usableSpaceKnown = knownSpace
                )
                suspend fun failAttempt() = AudioDownloadManager.handleDownloadAttemptFailure(
                    context, track, null, attemptId, operationId, false, state, failure
                )
                failAttempt()
                assertEquals(AudioDownloadManager.TRANSIENT_DOWNLOAD_MAX_ATTEMPTS, state.attemptNumber)
                val error = runCatching { failAttempt() }.exceptionOrNull()
                assertTrue(error is RetryableDownloadFailureException)
                error as RetryableDownloadFailureException
                assertEquals("DOWNLOAD_STORAGE_UNAVAILABLE", error.errorCode)
                assertFalse(error.networkUnavailable)
                assertArrayEquals(bytes, partial.readBytes())
                assertEquals(partial, state.tempFile)
            }
        } finally {
            AudioDownloadManager.operationRegistry.endSongDownloadOperation(track.stableKey(), operationId)
            AudioDownloadManager.clearVisibleProgressForSong(track.stableKey(), attemptId, operationId)
            GlobalDownloadManager.taskStore.removeDownloadTask(track.stableKey(), attemptId)
            partial.delete()
        }
    }

    @Test
    fun defaultProbeReportsUsableSpaceOnApplicationVolume() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val snapshot = DownloadStorageSpaceGuard().snapshot(context.cacheDir)

        assertTrue(snapshot.usableSpaceKnown)
        assertTrue(snapshot.usableBytes > 0L)
    }
}
