package com.mmc.merimusicplayer.data.local.storage.mapping

import com.mmc.merimusicplayer.data.local.database.store.DownloadIndexStorageStats
import com.mmc.merimusicplayer.data.local.database.store.PlatformPlaylistCacheStorageStats
import com.mmc.merimusicplayer.data.model.storage.StorageDownloadIndexStats
import com.mmc.merimusicplayer.data.model.storage.StoragePlatformCacheStats
import org.junit.Assert.assertEquals
import org.junit.Test

class StorageUsageInputsMapperTest {
    @Test
    fun databaseAdaptersPreserveRecordCountsAndAllocatedPages() {
        assertEquals(
            StorageDownloadIndexStats(118, 8192),
            DownloadIndexStorageStats(118, 8192).toStorageUsageStats()
        )
        assertEquals(
            StoragePlatformCacheStats(3, 4096),
            PlatformPlaylistCacheStorageStats(3, 4096).toStorageUsageStats()
        )
    }
}
