package com.mmc.merimusicplayer.data.model.download.execution

data class DownloadRetryPlan(
    val retryCount: Int,
    val nextRetryAtMs: Long?
)
