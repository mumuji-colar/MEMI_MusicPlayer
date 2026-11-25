package com.mmc.merimusicplayer.data.local.storage.accounting

import com.mmc.merimusicplayer.data.model.storage.StorageDownloadIndexStats
import com.mmc.merimusicplayer.data.model.storage.StoragePlatformCacheStats

internal data class DatabaseStorageAttribution(
    val platformCacheStats: Map<String, StoragePlatformCacheStats>,
    val downloadIndexStorageStats: StorageDownloadIndexStats
)
