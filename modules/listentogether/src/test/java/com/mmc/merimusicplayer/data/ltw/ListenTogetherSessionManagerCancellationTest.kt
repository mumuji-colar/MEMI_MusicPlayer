package com.mmc.merimusicplayer.data.ltw

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import com.mmc.merimusicplayer.data.ltw.lifecycle.cancelListenTogetherBackgroundJobs
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenTogetherSessionManagerCancellationTest {

    @Test
    fun `background reconnect jobs are cancelled together`() {
        val reconnectJob: CompletableJob = Job()
        val membershipRecoveryJob: CompletableJob = Job()

        cancelListenTogetherBackgroundJobs(reconnectJob, membershipRecoveryJob)

        assertTrue(reconnectJob.isCancelled)
        assertTrue(membershipRecoveryJob.isCancelled)
    }
}
