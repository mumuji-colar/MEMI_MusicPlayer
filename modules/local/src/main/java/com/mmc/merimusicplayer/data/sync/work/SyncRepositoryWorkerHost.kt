package com.mmc.merimusicplayer.data.sync.work

import com.mmc.merimusicplayer.data.model.sync.SyncProvider
import com.mmc.merimusicplayer.data.model.sync.SyncResult
import com.mmc.merimusicplayer.data.model.sync.SyncWorkerFailureKind
import com.mmc.merimusicplayer.data.model.sync.SyncWorkerOutcome
import com.mmc.merimusicplayer.data.sync.schedule.SyncWorkerFailureClassifier
import com.mmc.merimusicplayer.data.sync.schedule.SyncWorkerFailurePolicy
import com.mmc.merimusicplayer.data.sync.schedule.SyncWorkerHost
import com.mmc.merimusicplayer.data.sync.runtime.SyncProtocolUpgradeRequiredException

internal class SyncRepositoryWorkerHost(
    private val provider: SyncProvider,
    private val readAutoSync: () -> Boolean,
    private val readConfigured: () -> Boolean,
    private val readProtocolUpgradeApproved: suspend () -> Boolean,
    private val readPlayback: () -> Boolean,
    private val readNetwork: () -> Boolean,
    private val defer: () -> Unit,
    private val sync: suspend () -> Result<SyncResult>,
    private val classifier: SyncWorkerFailureClassifier,
    private val readSilentFailure: suspend () -> Boolean,
    private val notifyFailure: (Throwable?) -> Unit
) : SyncWorkerHost {
    override fun autoSyncEnabled(): Boolean = readAutoSync()
    override fun configured(): Boolean = readConfigured()
    override suspend fun protocolUpgradeApproved(): Boolean = readProtocolUpgradeApproved()
    override fun playbackActive(): Boolean = readPlayback()
    override fun validatedNetwork(): Boolean = readNetwork()
    override fun deferForPlayback() = defer()
    override suspend fun synchronize(): Result<SyncResult> = sync()

    override suspend fun handleFailure(error: Throwable?, manual: Boolean, unexpected: Boolean): SyncWorkerOutcome {
        if (error is SyncProtocolUpgradeRequiredException) return SyncWorkerOutcome.SUCCESS
        val kind = classifier.classify(error)
        val silent = shouldReadSilentPreference(kind, manual) && readSilentFailure()
        val decision = SyncWorkerFailurePolicy.decide(provider, kind, manual, unexpected, silent)
        if (decision.notify) notifyFailure(error)
        return decision.outcome
    }

    private fun shouldReadSilentPreference(kind: SyncWorkerFailureKind, manual: Boolean): Boolean =
        provider == SyncProvider.GITHUB && kind == SyncWorkerFailureKind.OTHER && !manual
}
