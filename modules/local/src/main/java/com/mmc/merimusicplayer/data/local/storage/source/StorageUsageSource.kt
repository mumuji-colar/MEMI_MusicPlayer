package com.mmc.merimusicplayer.data.local.storage.source

import com.mmc.merimusicplayer.data.model.storage.ManagedDownloadLibraryUsage
import com.mmc.merimusicplayer.data.model.storage.StorageDownloadIndexStats
import com.mmc.merimusicplayer.data.model.storage.StoragePlatformCacheStats

interface StorageUsageSource {
    suspend fun downloadLibraryUsage(): ManagedDownloadLibraryUsage
    suspend fun platformCacheStats(): Map<String, StoragePlatformCacheStats>
    suspend fun downloadIndexStats(): StorageDownloadIndexStats
}
