package com.mmc.merimusicplayer.data.local.platform.bilibili

import android.content.Context
import com.mmc.merimusicplayer.platform.bilibili.skip.BiliVideoSkipRepository
import com.mmc.merimusicplayer.data.local.database.maintenance.LegacyJsonCleanupRequests
import com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase
import com.mmc.merimusicplayer.platform.bilibili.skip.storage.BiliVideoSkipRoomStore
import com.mmc.merimusicplayer.data.sync.github.GitHubSyncWorker
import com.mmc.merimusicplayer.data.sync.store.github.SecureTokenStorage
import com.mmc.merimusicplayer.data.sync.webdav.WebDavSyncWorker

object BiliVideoSkipRepositoryProvider {
    @Volatile
    private var instance: BiliVideoSkipRepository? = null

    fun getInstance(context: Context): BiliVideoSkipRepository {
        return instance ?: synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }
    }

    private fun create(context: Context): BiliVideoSkipRepository {
        val storage = SecureTokenStorage(context)
        return BiliVideoSkipRepository(
            store = BiliVideoSkipRoomStore(MemiUserDataDatabase.getInstance(context)),
            legacyDirectory = context.filesDir,
            readSyncMutationVersion = storage::getSyncMutationVersion,
            onRulesChanged = {
                storage.markSyncMutation()
                GitHubSyncWorker.scheduleDelayedSync(
                    context,
                    triggerByUserAction = false,
                    markMutation = false
                )
                WebDavSyncWorker.scheduleDelayedSync(
                    context,
                    triggerByUserAction = false,
                    markMutation = false
                )
            },
            scheduleLegacyCleanup = { reason ->
                LegacyJsonCleanupRequests.schedule(context, reason)
            }
        )
    }
}
