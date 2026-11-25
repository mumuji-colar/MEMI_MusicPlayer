package com.mmc.merimusicplayer.core.download.execution.recovery

import com.mmc.merimusicplayer.core.download.execution.host.DownloadOperationEntryPoint
import com.mmc.merimusicplayer.data.model.download.DownloadExecutionRequest
import com.mmc.merimusicplayer.data.model.download.DownloadExecutionResult
import android.content.Context
import com.mmc.merimusicplayer.core.download.GlobalDownloadManager

/** 将兼容 facade 的启动调用集中到一个可替换入口 */
internal object ExistingDownloadOperationEntryPoint : DownloadOperationEntryPoint {
    override suspend fun start(
        context: Context,
        request: DownloadExecutionRequest
    ): DownloadExecutionResult {
        return GlobalDownloadManager.startDownload(
            context = context,
            song = request.song,
            operationId = request.operationId,
            preserveStaging = request.preserveStaging,
            preparedAttemptId = request.attemptId
        )
    }
}
