package com.mmc.merimusicplayer.data.sync.work

import android.content.Context
import com.mmc.merimusicplayer.api.sync.webdav.WebDavAccessDeniedException
import com.mmc.merimusicplayer.api.sync.webdav.WebDavAuthException
import com.mmc.merimusicplayer.api.sync.webdav.WebDavDirectoryNotFoundException
import com.mmc.merimusicplayer.api.sync.webdav.WebDavMissingConcurrencyTokenException
import com.mmc.merimusicplayer.api.sync.webdav.WebDavNotDirectoryException
import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.common.locale.LanguageManager
import com.mmc.merimusicplayer.data.model.sync.SyncProvider
import com.mmc.merimusicplayer.data.model.sync.SyncWorkerFailureKind
import com.mmc.merimusicplayer.data.sync.host.SyncPlaybackActivity
import com.mmc.merimusicplayer.data.sync.host.SyncProtocolUpgradeRepository
import com.mmc.merimusicplayer.data.sync.schedule.SyncWorkerFailureClassifier
import com.mmc.merimusicplayer.data.sync.schedule.SyncWorkerHost
import com.mmc.merimusicplayer.data.sync.store.webdav.WebDavStorage
import com.mmc.merimusicplayer.data.sync.webdav.WebDavSyncInProgressException
import com.mmc.merimusicplayer.data.sync.webdav.WebDavSyncManager
import com.mmc.merimusicplayer.data.sync.webdav.WebDavSyncWorker

internal fun createWebDavWorkerHost(context: Context, triggerByAppStartup: Boolean = false): SyncWorkerHost {
    val storage by lazy { WebDavStorage(context) }
    val notification = SyncFailureNotification(
        context, "webdav_sync_channel", 1002,
        CoreCommonR.string.webdav_sync_channel_name, CoreCommonR.string.webdav_sync_channel_desc, CoreCommonR.string.webdav_sync_failed_title
    )
    return SyncRepositoryWorkerHost(
        provider = SyncProvider.WEBDAV,
        readAutoSync = { storage.isAutoSyncEnabled() }, readConfigured = { storage.isConfigured() },
        readProtocolUpgradeApproved = {
            SyncProtocolUpgradeRepository(context).canSyncTarget(SyncProtocolUpgradeRepository.webDavTargetHash(
                storage.getServerUrl().orEmpty(), storage.getBasePath(), storage.getUsername().orEmpty()
            ))
        },
        readPlayback = { SyncPlaybackActivity.isActive }, readNetwork = { hasValidatedSyncNetwork(context) },
        defer = { WebDavSyncWorker.scheduleDelayedSync(context, initialDelayMs = 60_000L, appendToCurrentWork = true) },
        sync = {
            val manager = WebDavSyncManager.getInstance(context)
            if (triggerByAppStartup) manager.performSync(triggerByAppStartup) else manager.performSync()
        },
        classifier = SyncWorkerFailureClassifier(mapOf(
            WebDavAuthException::class.java to SyncWorkerFailureKind.AUTHENTICATION,
            WebDavDirectoryNotFoundException::class.java to SyncWorkerFailureKind.CONFIGURATION,
            WebDavNotDirectoryException::class.java to SyncWorkerFailureKind.CONFIGURATION,
            WebDavAccessDeniedException::class.java to SyncWorkerFailureKind.CONFIGURATION,
            WebDavMissingConcurrencyTokenException::class.java to SyncWorkerFailureKind.MISSING_CONDITION,
            WebDavSyncInProgressException::class.java to SyncWorkerFailureKind.ALREADY_RUNNING
        )),
        readSilentFailure = { false }, notifyFailure = { notification.show(webDavFailureMessage(context, it)) }
    )
}

private fun webDavFailureMessage(context: Context, error: Throwable?): String {
    val localized = LanguageManager.applyLanguage(context.applicationContext)
    return if (error is WebDavAuthException) localized.getString(CoreCommonR.string.webdav_auth_failed)
    else error?.message ?: localized.getString(CoreCommonR.string.webdav_sync_failed_message)
}
