package com.mmc.merimusicplayer.data.local.storage.source

import com.mmc.merimusicplayer.data.model.storage.ExtraCacheClearResult
import com.mmc.merimusicplayer.data.model.storage.FileStats

interface LocalImportCacheAccess {
    suspend fun scan(): FileStats
    suspend fun clear(): ExtraCacheClearResult
}
