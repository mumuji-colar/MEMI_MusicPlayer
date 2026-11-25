package com.mmc.merimusicplayer.data.local.storage.mapping

import com.mmc.merimusicplayer.data.model.download.DownloadLibrarySnapshot
import com.mmc.merimusicplayer.data.model.download.DownloadLibraryEntry

import com.mmc.merimusicplayer.data.local.storage.accounting.managedDownloadLibraryUsage
import com.mmc.merimusicplayer.data.model.storage.ManagedDownloadLibraryUsage
import com.mmc.merimusicplayer.data.model.storage.StorageDownloadIndexStats
import com.mmc.merimusicplayer.data.model.storage.StorageLibraryEntry
import com.mmc.merimusicplayer.data.model.storage.StoragePlatformCacheStats

import com.mmc.merimusicplayer.data.local.database.store.DownloadIndexStorageStats
import com.mmc.merimusicplayer.data.local.database.store.PlatformPlaylistCacheStorageStats

fun DownloadLibrarySnapshot.toManagedDownloadLibraryUsage():
    ManagedDownloadLibraryUsage {
    return managedDownloadLibraryUsage(
        audioEntries = audioEntries.map { it.toStorageLibraryEntry() },
        lyricEntries = lyricEntriesByName.values.map { it.toStorageLibraryEntry() },
        coverEntries = coverEntriesByName.values.map { it.toStorageLibraryEntry() },
        metadataEntries = metadataEntriesByAudioName.values.map { it.toStorageLibraryEntry() }
    )
}

fun DownloadLibraryEntry.toStorageLibraryEntry(): StorageLibraryEntry =
    StorageLibraryEntry(
        name = name,
        reference = reference,
        mediaUri = mediaUri,
        localFilePath = localFilePath,
        sizeBytes = sizeBytes,
        isDirectory = isDirectory
    )

internal fun DownloadIndexStorageStats.toStorageUsageStats(): StorageDownloadIndexStats =
    StorageDownloadIndexStats(databaseRecordCount = databaseRecordCount, allocatedPageBytes = allocatedPageBytes)

fun PlatformPlaylistCacheStorageStats.toStorageUsageStats(): StoragePlatformCacheStats =
    StoragePlatformCacheStats(cacheRecordCount = cacheRecordCount, allocatedPageBytes = allocatedPageBytes)
