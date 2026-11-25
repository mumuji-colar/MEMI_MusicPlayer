package com.mmc.merimusicplayer.core.download.execution.persistence

import com.mmc.merimusicplayer.data.identity.stableKey

import android.content.Context
import com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase
import com.mmc.merimusicplayer.data.model.stableKey

/**
 * Room operation 的状态查询边界，避免状态读取和取消写入互相耦合
 */
internal object DownloadExecutionRoomStatusStore {
    suspend fun isStopped(
        context: Context,
        operationId: String
    ): Boolean {
        return MemiUserDataDatabase.getInstance(context).downloadOperationDao()
            .isUserStopped(operationId) == true
    }

    suspend fun isUserCancellationRequested(
        context: Context,
        operationId: String
    ): Boolean {
        return MemiUserDataDatabase.getInstance(context).downloadOperationDao()
            .isUserCancellationRequested(operationId)
    }

    suspend fun isExplicitResumePending(
        context: Context,
        operationId: String,
        database: MemiUserDataDatabase = MemiUserDataDatabase.getInstance(context)
    ): Boolean {
        return database.downloadOperationDao().isExplicitResumePending(operationId)
    }

    suspend fun isExecutionOwned(
        context: Context,
        operationId: String,
        stableKey: String,
        database: MemiUserDataDatabase = MemiUserDataDatabase.getInstance(context)
    ): Boolean {
        val normalizedKey = stableKey.trim().takeIf(String::isNotBlank) ?: return false
        return database.downloadOperationDao().isExecutionOwnedAnyLibrary(
            operationId = operationId,
            stableKey = normalizedKey
        )
    }

    suspend fun stoppedSongKeys(context: Context): Set<String> {
        val dao = MemiUserDataDatabase.getInstance(context).downloadOperationDao()
        return dao.findUserStoppedHeaders()
            .mapNotNull { header ->
                DownloadExecutionRoomStore.Access.readRequestFromHeader(dao, header).request?.song?.stableKey()
            }
            .toSet()
    }

}
