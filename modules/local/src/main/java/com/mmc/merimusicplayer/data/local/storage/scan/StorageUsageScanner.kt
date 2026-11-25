package com.mmc.merimusicplayer.data.local.storage.scan

import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.mmc.merimusicplayer.data.local.storage.accounting.databaseUsageStats
import com.mmc.merimusicplayer.data.local.storage.accounting.downloadIndexUsageStats
import com.mmc.merimusicplayer.data.local.storage.accounting.normalizeDatabaseStorageAttribution
import com.mmc.merimusicplayer.data.local.storage.accounting.DatabaseStorageAttribution
import com.mmc.merimusicplayer.data.model.storage.FileStats
import com.mmc.merimusicplayer.data.model.storage.ManagedDownloadLibraryUsage
import com.mmc.merimusicplayer.data.model.storage.StorageDownloadIndexStats
import com.mmc.merimusicplayer.data.model.storage.StoragePlatformCacheStats
import com.mmc.merimusicplayer.data.model.storage.StorageUsageItemKind
import com.mmc.merimusicplayer.data.model.storage.StorageUsageMeasurement
import com.mmc.merimusicplayer.data.model.storage.StorageUsageSnapshot
import com.mmc.merimusicplayer.data.local.storage.policy.storageScanOrDefault
import com.mmc.merimusicplayer.data.local.storage.source.StorageLocations
import com.mmc.merimusicplayer.data.local.storage.source.LocalImportCacheAccess
import com.mmc.merimusicplayer.data.local.storage.source.StorageUsageSource
import com.mmc.merimusicplayer.data.local.storage.source.storageCacheItemKinds
import com.mmc.merimusicplayer.data.local.storage.source.storagePlatformCaches

class StorageUsageScanner(
    private val locations: StorageLocations,
    private val source: StorageUsageSource,
    private val localImport: LocalImportCacheAccess? = null
) {
    suspend fun scan(): StorageUsageSnapshot = coroutineScope {
        val library = async { storageScanOrDefault(ManagedDownloadLibraryUsage.Empty) { source.downloadLibraryUsage() } }
        val platform = async { storageScanOrDefault(emptyMap()) { source.platformCacheStats() } }
        val index = async { storageScanOrDefault(StorageDownloadIndexStats.Empty) { source.downloadIndexStats() } }
        val database = async { statsOfFiles(locations.databaseFiles) }
        val importCache = async { storageScanOrDefault(FileStats.Empty) { localImport?.scan() ?: FileStats.Empty } }
        assembleSnapshot(library.await(), platform.await(), index.await(), database.await(), importCache.await())
    }

    private fun assembleSnapshot(
        library: ManagedDownloadLibraryUsage,
        platform: Map<String, StoragePlatformCacheStats>,
        index: StorageDownloadIndexStats,
        database: FileStats,
        importCache: FileStats
    ): StorageUsageSnapshot {
        val attributed = normalizeDatabaseStorageAttribution(platform, index, database.sizeBytes)
        val items = cacheMeasurements(attributed.platformCacheStats).toMutableMap()
        items.putAll(libraryMeasurements(library, attributed.downloadIndexStorageStats))
        items.putAll(appDataMeasurements(library, attributed, database))
        val allCovers = items.getValue(StorageUsageItemKind.LocalCovers)
        val cleanableCovers = FileStats(
            importCache.sizeBytes.coerceIn(0, allCovers.stats.sizeBytes),
            importCache.fileCount.coerceIn(0, allCovers.stats.fileCount)
        )
        items[StorageUsageItemKind.LocalImportCache] = StorageUsageMeasurement(cleanableCovers, locations.localCoverDir.absolutePath)
        items[StorageUsageItemKind.LocalCovers] = allCovers.copy(stats = FileStats(
            allCovers.stats.sizeBytes - cleanableCovers.sizeBytes,
            allCovers.stats.fileCount - cleanableCovers.fileCount
        ))
        items[StorageUsageItemKind.ProtectedCache] = StorageUsageMeasurement(
            measuredStatsOf(locations.cacheDir, locations.cacheExclusions()), locations.cacheDir.absolutePath
        )
        return StorageUsageSnapshot(items.toMap())
    }

    private fun cacheMeasurements(platform: Map<String, StoragePlatformCacheStats>) =
        locations.cacheFiles.entries.associate { (kind, files) ->
            storageCacheItemKinds.getValue(kind) to cacheMeasurement(files, platform[storagePlatformCaches[kind]])
        }

    private fun cacheMeasurement(files: List<File>, room: StoragePlatformCacheStats?): StorageUsageMeasurement {
        val stats = statsOfFiles(files)
        return StorageUsageMeasurement(
            stats = stats.copy(sizeBytes = stats.sizeBytes + (room?.allocatedPageBytes ?: 0L)),
            path = files.joinToString("\n") { it.absolutePath },
            databaseRecordCount = room?.cacheRecordCount
        )
    }

    private fun libraryMeasurements(library: ManagedDownloadLibraryUsage, index: StorageDownloadIndexStats):
        Map<StorageUsageItemKind, StorageUsageMeasurement> {
        val indexStats = downloadIndexUsageStats(library.metadataFiles + statsOfFiles(locations.downloadMetadataFiles), index)
        return mapOf(
            StorageUsageItemKind.DownloadedMusic to StorageUsageMeasurement(library.audioFiles),
            StorageUsageItemKind.DownloadedLyrics to StorageUsageMeasurement(library.lyricFiles),
            StorageUsageItemKind.DownloadedCovers to StorageUsageMeasurement(library.coverFiles),
            StorageUsageItemKind.DownloadIndex to StorageUsageMeasurement(
                FileStats(indexStats.sizeBytes, indexStats.fileCount), databaseRecordCount = indexStats.databaseRecordCount
            )
        )
    }

    private fun appDataMeasurements(library: ManagedDownloadLibraryUsage, attributed: DatabaseStorageAttribution, database: FileStats):
        Map<StorageUsageItemKind, StorageUsageMeasurement> {
        val attributedBytes = attributed.platformCacheStats.values.sumOf { it.allocatedPageBytes } +
            attributed.downloadIndexStorageStats.allocatedPageBytes
        return mapOf(
            StorageUsageItemKind.LocalCovers to fileMeasurement(locations.localCoverDir),
            StorageUsageItemKind.CustomBackground to fileMeasurement(locations.backgroundDir),
            StorageUsageItemKind.LegacyMigrationFiles to StorageUsageMeasurement(
                statsOfFiles(locations.playlistDataFiles), locations.playlistDataFiles.joinToString("\n") { it.absolutePath }
            ),
            StorageUsageItemKind.Database to StorageUsageMeasurement(databaseUsageStats(database, attributedBytes)),
            StorageUsageItemKind.AppData to StorageUsageMeasurement(
                measuredStatsOf(locations.filesDir, locations.appDataExclusions(library.localFiles)), locations.filesDir.absolutePath
            )
        )
    }

    private fun fileMeasurement(file: File) = StorageUsageMeasurement(measuredStatsOf(file), file.absolutePath)
}
