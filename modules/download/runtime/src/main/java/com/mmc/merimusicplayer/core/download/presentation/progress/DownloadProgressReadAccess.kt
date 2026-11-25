package com.mmc.merimusicplayer.core.download.presentation.progress

import android.content.Context
import com.mmc.merimusicplayer.core.download.admission.DownloadClearVisibility
import com.mmc.merimusicplayer.core.download.execution.clear.PersistentDownloadClearFenceStore
import com.mmc.merimusicplayer.core.download.execution.clear.PersistentDownloadClearProgressStore
import com.mmc.merimusicplayer.core.download.execution.persistence.DownloadExecutionRoomStore
import com.mmc.merimusicplayer.core.download.execution.persistence.WAITING_STORAGE_MUTATION_OPERATION_STATE
import com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase
import com.mmc.merimusicplayer.data.local.database.entity.DownloadOperationHeaderRow
import com.mmc.merimusicplayer.data.model.download.DownloadExecutionRequest

object DownloadProgressReadAccess {
    const val WAITING_STORAGE_MUTATION_STATE = WAITING_STORAGE_MUTATION_OPERATION_STATE
    val reusableOperationStates: List<String> get() = DownloadExecutionRoomStore.REUSABLE_OPERATION_STATES

    fun isTaskProgressClearActive(context: Context): Boolean =
        PersistentDownloadClearFenceStore.isTaskProgressActive(context)

    fun readClearProgress(context: Context): DownloadClearVisibility.ClearProgress? =
        PersistentDownloadClearProgressStore.read(context)

    suspend fun readOperationHeaders(
        context: Context,
        operationIds: Collection<String>,
        database: MemiUserDataDatabase
    ): Map<String, DownloadOperationHeaderRow> =
        DownloadExecutionRoomStore.readOperationHeaders(context, operationIds, database)

    suspend fun listPendingRequests(
        context: Context,
        states: List<String>,
        excludeUserStoppedOperations: Boolean,
        database: MemiUserDataDatabase
    ): List<DownloadExecutionRequest> = DownloadExecutionRoomStore.listByStates(
        context, states, excludeUserStoppedOperations, database
    ).map { entry -> entry.request }
}
