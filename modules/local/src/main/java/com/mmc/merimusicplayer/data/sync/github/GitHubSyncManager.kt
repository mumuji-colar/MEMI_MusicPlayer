package com.mmc.merimusicplayer.data.sync.github

import com.mmc.merimusicplayer.data.sync.dataset.disk.FileSyncPlaybackDatasetStore
import com.mmc.merimusicplayer.data.sync.store.github.SecureTokenStorage

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.mmc.merimusicplayer.data.sync.github/GitHubSyncManager
 * Updated: 2026/3/23
 */

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.data.model.sync.SyncResult
import com.mmc.merimusicplayer.data.sync.host.createGitHubSyncBackend
import com.mmc.merimusicplayer.data.sync.host.SyncServiceInstance
import com.mmc.merimusicplayer.data.sync.host.AndroidSyncLocalDataStore
import com.mmc.merimusicplayer.data.sync.host.AndroidSyncMergeHost
import com.mmc.merimusicplayer.data.sync.merge.engine.SyncDataMerger
import com.mmc.merimusicplayer.data.sync.runtime.SyncSession
import java.io.File
import com.mmc.merimusicplayer.data.sync.host.SyncProtocolUpgradeRepository
import com.mmc.merimusicplayer.data.sync.host.verifyTargetSyncCompletion
import com.mmc.merimusicplayer.common.locale.LanguageManager
import com.mmc.merimusicplayer.common.logging.NPLogger

class GitHubSyncManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val storage by lazy { SecureTokenStorage(appContext) }
    private val local by lazy { AndroidSyncLocalDataStore(appContext, storage) }
    private val protocolUpgrade = SyncProtocolUpgradeRepository(appContext)

    companion object {
        private val instance = SyncServiceInstance<GitHubSyncManager>()

        fun getInstance(context: Context): GitHubSyncManager =
            instance.get { GitHubSyncManager(context.applicationContext) }
    }

    suspend fun performSync(): Result<SyncResult> = executeSync(null)

    suspend fun performSync(triggerByAppStartup: Boolean): Result<SyncResult> = executeSync(null, triggerByAppStartup)

    suspend fun performSyncForTarget(targetId: String): Result<SyncResult> =
        verifyTargetSyncCompletion(executeSync(targetId), targetId) { target ->
            protocolUpgrade.versionFlow(target).first()
        }

    private suspend fun executeSync(expectedTargetId: String?, triggerByAppStartup: Boolean = false): Result<SyncResult> = protocolUpgrade.executeIfApproved {
        withContext(Dispatchers.IO) {
            val started = System.nanoTime()
            var outcome = "failed"
            try {
                executeInitializedSync(expectedTargetId, triggerByAppStartup).also { result ->
                    outcome = syncOutcome(result)
                }
            } catch (cancelled: CancellationException) {
                outcome = "cancelled"
                throw cancelled
            } finally {
                NPLogger.i("GitHubSyncManager", "GitHub sync total: ${(System.nanoTime() - started) / 1_000_000L} ms, startup=$triggerByAppStartup, outcome=$outcome")
            }
        }
    }

    internal fun syncOutcome(result: Result<SyncResult>): String = result.fold(
        onSuccess = { if (it.success) "success" else "deferred" },
        onFailure = { "failed:${it.javaClass.simpleName}" }
    )

    private suspend fun executeInitializedSync(expectedTargetId: String?, triggerByAppStartup: Boolean): Result<SyncResult> {
        val localizedContext = LanguageManager.applyLanguage(appContext)
        return SyncSession(
            local = local,
            datasetStore = FileSyncPlaybackDatasetStore(File(appContext.cacheDir, "sync-v3/playback-staging")),
            merger = SyncDataMerger(AndroidSyncMergeHost(localizedContext, CoreCommonR.string.github_sync_success_detail)),
            noChangeMessage = localizedContext.getString(CoreCommonR.string.github_sync_no_change),
            initialUploadMessage = localizedContext.getString(CoreCommonR.string.sync_initial_uploaded),
            deferredMessage = localizedContext.getString(CoreCommonR.string.sync_local_changes_pending),
            inProgressError = { GitHubSyncInProgressException(localizedContext.getString(CoreCommonR.string.github_sync_in_progress)) }
        ).execute { createGitHubSyncBackend(appContext, storage, expectedTargetId, triggerByAppStartup) }
    }
}
