package com.mmc.merimusicplayer.core.download.manager.batch

import com.mmc.merimusicplayer.data.identity.stableKey

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.mmc.merimusicplayer.core.download.GlobalDownloadManager
import com.mmc.merimusicplayer.core.download.execution.clear.ManagedDownloadDirectoryMutationFence
import com.mmc.merimusicplayer.data.model.download.DownloadExecutionRequest
import com.mmc.merimusicplayer.core.download.execution.persistence.DownloadExecutionRoomStore
import com.mmc.merimusicplayer.core.download.manager.admission.admitDownloadMutationForStableKeys
import com.mmc.merimusicplayer.core.download.manager.admission.isDownloadAdmissionTicketCurrent
import com.mmc.merimusicplayer.core.download.manager.admission.promoteUserInitiatedInFlightRequests
import com.mmc.merimusicplayer.core.download.manager.runtime.wakeDownloadExecutionPump
import com.mmc.merimusicplayer.data.model.stableKey

// 只串行批次建队和首窗准备，传输宿主与取消路径不获取这把锁
private val batchDownloadStartupMutex = Mutex()

internal fun <T : Any> CoroutineScope.launchBatchDownloadStartup(
    beforeStartup: suspend CoroutineScope.() -> T?,
    block: suspend CoroutineScope.(T) -> Unit
): Job = launch {
    val input = beforeStartup() ?: return@launch
    batchDownloadStartupMutex.withLock {
        block(input)
    }
}

internal suspend fun GlobalDownloadManager.resumeOwnedBatchDownloadRequests(
    context: Context,
    requests: List<DownloadExecutionRequest>,
    admissionTicket: Long,
    userInitiated: Boolean
) {
    if (requests.isEmpty()) return
    var inFlightRequests = emptyList<DownloadExecutionRequest>()
    val admitted = admitDownloadMutationForStableKeys(
        context, admissionTicket, requests.map { it.song.stableKey() }
    ) { admittedKeys ->
        val currentRequests = requests.filter { request ->
            request.song.stableKey() in admittedKeys && isDownloadAdmissionTicketCurrent(
                context, admissionTicket, request.song.stableKey(), request.operationId
            ) && request.operationId !in cancellationOperationIdsForSong(request.song.stableKey())
        }
        val promoted = promoteUserInitiatedInFlightRequests(context, currentRequests, userInitiated)
        val operationIds = promoted.map(DownloadExecutionRequest::operationId)
        if (!ManagedDownloadDirectoryMutationFence.isActive(context)) {
            operationIds.chunked(DownloadExecutionRoomStore.SQLITE_IN_QUERY_CHUNK_SIZE).forEach { ids ->
                DownloadExecutionRoomStore.promoteWaitingStorageMutations(context, ids)
            }
        }
        val headers = DownloadExecutionRoomStore.readOperationHeaders(context, operationIds)
        if (headers.values.any { it.state in DownloadExecutionRoomStore.REUSABLE_OPERATION_STATES }) {
            wakeDownloadExecutionPump(context, reason = "existing_batch_selected")
        }
        inFlightRequests = promoted.filter { request ->
            headers[request.operationId]?.state in DownloadExecutionRoomStore.IN_FLIGHT_OPERATION_STATES
        }
    }
    if (admitted) {
        recoverInFlightDownloadOperations(context, inFlightRequests, admissionTicket)
    }
}
