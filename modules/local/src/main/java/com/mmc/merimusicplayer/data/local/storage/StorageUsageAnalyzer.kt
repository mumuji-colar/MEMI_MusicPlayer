package com.mmc.merimusicplayer.data.local.storage

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.mmc.merimusicplayer.data.local.storage/StorageUsageAnalyzer
 * Created: 2026/7/9
 */

import com.mmc.merimusicplayer.data.local.storage.host.AndroidStorageCacheFiles
import com.mmc.merimusicplayer.data.local.storage.host.AndroidStoragePlatformCaches
import com.mmc.merimusicplayer.data.local.storage.host.AndroidStorageUsageSource
import com.mmc.merimusicplayer.data.local.storage.host.androidLocalImportCache
import com.mmc.merimusicplayer.data.local.storage.host.storageLocations
import com.mmc.merimusicplayer.data.local.storage.presentation.StorageUsagePresenter
import com.mmc.merimusicplayer.data.local.storage.cleanup.StorageCacheCleaner
import com.mmc.merimusicplayer.data.model.storage.ExtraCacheClearResult
import com.mmc.merimusicplayer.data.model.storage.StorageCacheClearOptions
import com.mmc.merimusicplayer.data.model.storage.StorageUsageSummary
import com.mmc.merimusicplayer.data.local.storage.scan.StorageUsageScanner
import android.content.Context
import com.mmc.merimusicplayer.data.model.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun analyzeStorageUsage(
    context: Context,
    additionalSongsProvider: () -> List<SongItem> = { emptyList() }
): StorageUsageSummary = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    val snapshot = StorageUsageScanner(
        storageLocations(appContext), AndroidStorageUsageSource(appContext), androidLocalImportCache(appContext, additionalSongsProvider)
    ).scan()
    StorageUsagePresenter(appContext.resources).present(snapshot)
}

suspend fun clearExtraStorageCaches(
    context: Context,
    options: StorageCacheClearOptions,
    additionalSongsProvider: () -> List<SongItem> = { emptyList() }
): ExtraCacheClearResult = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    StorageCacheCleaner(
        storageLocations(appContext),
        AndroidStorageCacheFiles(appContext),
        AndroidStoragePlatformCaches(appContext),
        androidLocalImportCache(appContext, additionalSongsProvider)
    ).clear(options)
}
