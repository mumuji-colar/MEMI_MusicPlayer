package com.mmc.merimusicplayer.data.local.storage.source

import java.io.File
import com.mmc.merimusicplayer.data.model.storage.FileStats
import com.mmc.merimusicplayer.data.model.storage.StorageCacheKind

interface StorageCacheFileAccess {
    fun stats(file: File): FileStats
    suspend fun clear(file: File, kind: StorageCacheKind): Boolean
}

interface StoragePlatformCacheAccess {
    suspend fun allocatedBytes(platforms: List<String>): Long
    suspend fun clear(platforms: List<String>)
}
