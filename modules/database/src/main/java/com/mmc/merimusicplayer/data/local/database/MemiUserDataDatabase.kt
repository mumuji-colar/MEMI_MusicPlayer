package com.mmc.merimusicplayer.data.local.database

import android.app.Application
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.mmc.merimusicplayer.data.local.database.dao.LocalPlaylistDao
import com.mmc.merimusicplayer.data.local.database.dao.PlayHistoryDao
import com.mmc.merimusicplayer.data.local.database.dao.stats.PlaylistUsageDao
import com.mmc.merimusicplayer.data.local.database.dao.stats.LocalPlaylistPlaybackDao
import com.mmc.merimusicplayer.data.local.database.dao.stats.PlaybackStatsSnapshotDao
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatsSnapshotEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatsSnapshotTrackEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatsSnapshotBucketEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatsSnapshotCounterEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatsSnapshotDailyCounterEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatsSnapshotDeletedTrackEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatsSnapshotDeletedBucketEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatsPendingDeltaEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatsEventReceiptEntity
import com.mmc.merimusicplayer.data.local.database.migration.library.PlaybackStatsPagedMigration
import com.mmc.merimusicplayer.data.local.database.dao.stats.PlaybackStatsDao
import com.mmc.merimusicplayer.data.local.database.dao.FavoritePlaylistDao
import com.mmc.merimusicplayer.data.local.database.dao.stats.TrafficStatsDao
import com.mmc.merimusicplayer.data.local.database.dao.SyncMetadataDao
import com.mmc.merimusicplayer.data.local.database.dao.PlaybackQueueDao
import com.mmc.merimusicplayer.data.local.database.dao.BiliVideoSkipDao
import com.mmc.merimusicplayer.data.local.database.dao.CoverUrlMappingDao
import com.mmc.merimusicplayer.data.local.database.dao.DownloadOperationDao
import com.mmc.merimusicplayer.data.local.database.dao.DownloadBatchDao
import com.mmc.merimusicplayer.data.local.database.dao.ManagedLibraryItemDao
import com.mmc.merimusicplayer.data.local.database.dao.ManagedDownloadArtifactDao
import com.mmc.merimusicplayer.data.local.database.dao.PlatformPlaylistCacheDao
import com.mmc.merimusicplayer.data.local.database.entity.FavoritePlaylistEntity
import com.mmc.merimusicplayer.data.local.database.entity.FavoritePlaylistSongEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.TrafficStatsBucketEntity
import com.mmc.merimusicplayer.data.local.database.entity.LocalPlaylistEntity
import com.mmc.merimusicplayer.data.local.database.entity.MigrationMetadataEntity
import com.mmc.merimusicplayer.data.local.database.entity.PlayHistoryEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaylistUsageCounterShardEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaylistUsageEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.LocalPlaylistPlaybackBucketEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.LocalPlaylistPlaybackCounterShardEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.LocalPlaylistPlaybackStatEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatBucketEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatCounterShardEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatDailyCounterShardEntity
import com.mmc.merimusicplayer.data.local.database.entity.stats.PlaybackStatEntity
import com.mmc.merimusicplayer.data.local.database.entity.PlaylistMemberEntity
import com.mmc.merimusicplayer.data.local.database.entity.PlaylistMemberTokenEntity
import com.mmc.merimusicplayer.data.local.database.entity.SyncOutboxEntity
import com.mmc.merimusicplayer.data.local.database.entity.SyncReplicaCheckpointEntity
import com.mmc.merimusicplayer.data.local.database.entity.TrackEntity
import com.mmc.merimusicplayer.data.local.database.entity.PlaybackQueueSongEntity
import com.mmc.merimusicplayer.data.local.database.entity.PlaybackQueueStateEntity
import com.mmc.merimusicplayer.data.local.database.entity.BiliVideoSkipDraftEntity
import com.mmc.merimusicplayer.data.local.database.entity.BiliVideoSkipIntervalEntity
import com.mmc.merimusicplayer.data.local.database.entity.BiliVideoSkipRuleEntity
import com.mmc.merimusicplayer.data.local.database.entity.CoverUrlMappingEntity
import com.mmc.merimusicplayer.data.local.database.entity.DownloadOperationEntity
import com.mmc.merimusicplayer.data.local.database.entity.DownloadBatchEntity
import com.mmc.merimusicplayer.data.local.database.entity.DownloadBatchMemberEntity
import com.mmc.merimusicplayer.data.local.database.entity.ManagedLibraryItemEntity
import com.mmc.merimusicplayer.data.local.database.entity.PlatformPlaylistCacheEntity
import com.mmc.merimusicplayer.data.local.database.entity.PlatformPlaylistCacheTrackArtistEntity
import com.mmc.merimusicplayer.data.local.database.entity.PlatformPlaylistCacheTrackEntity

import com.mmc.merimusicplayer.data.local.database.migration.library.PlayHistoryMigration
import com.mmc.merimusicplayer.data.local.database.migration.library.LyricSyncPersistenceMigration
import com.mmc.merimusicplayer.data.local.database.migration.library.LocalSourceProvenanceMigration
import com.mmc.merimusicplayer.data.local.database.migration.library.PlaylistUsageMigration
import com.mmc.merimusicplayer.data.local.database.migration.library.LocalPlaylistPlaybackMigration
import com.mmc.merimusicplayer.data.local.database.migration.library.PlaybackStatsMigration
import com.mmc.merimusicplayer.data.local.database.migration.library.FavoritePlaylistMigration
import com.mmc.merimusicplayer.data.local.database.migration.library.TrafficStatsMigration
import com.mmc.merimusicplayer.data.local.database.migration.library.PlaybackQueueMigration
import com.mmc.merimusicplayer.data.local.database.migration.platform.BiliVideoSkipMigration
import com.mmc.merimusicplayer.data.local.database.migration.download.DownloadQueueMigration
import com.mmc.merimusicplayer.data.local.database.migration.download.DownloadedSongCatalogMigration
import com.mmc.merimusicplayer.data.local.database.migration.library.CoverUrlMappingMigration
import com.mmc.merimusicplayer.data.local.database.migration.download.DownloadSnapshotMigration
import com.mmc.merimusicplayer.data.local.database.migration.platform.PlatformPlaylistCacheMigration
import com.mmc.merimusicplayer.data.local.database.migration.download.DownloadRomanizedLyricsMigration
import com.mmc.merimusicplayer.data.local.database.migration.download.DownloadFinalizationMigration
import com.mmc.merimusicplayer.data.local.database.migration.download.DownloadBatchMigration
import com.mmc.merimusicplayer.data.local.database.migration.download.DownloadRecoveryCursorMigration

private const val NERI_USER_DATA_FINAL_VERSION = 21

@Database(
    entities = [
        LocalPlaylistEntity::class,
        TrackEntity::class,
        PlaylistMemberEntity::class,
        PlaylistMemberTokenEntity::class,
        SyncOutboxEntity::class,
        SyncReplicaCheckpointEntity::class,
        MigrationMetadataEntity::class,
        PlayHistoryEntity::class,
        PlaylistUsageEntity::class,
        PlaylistUsageCounterShardEntity::class,
        LocalPlaylistPlaybackStatEntity::class,
        LocalPlaylistPlaybackBucketEntity::class,
        LocalPlaylistPlaybackCounterShardEntity::class,
        PlaybackStatEntity::class,
        PlaybackStatsSnapshotEntity::class,
        PlaybackStatsSnapshotTrackEntity::class,
        PlaybackStatsSnapshotBucketEntity::class,
        PlaybackStatsSnapshotCounterEntity::class,
        PlaybackStatsSnapshotDailyCounterEntity::class,
        PlaybackStatsSnapshotDeletedTrackEntity::class,
        PlaybackStatsSnapshotDeletedBucketEntity::class,
        PlaybackStatsPendingDeltaEntity::class,
        PlaybackStatsEventReceiptEntity::class,
        PlaybackStatBucketEntity::class,
        PlaybackStatCounterShardEntity::class,
        PlaybackStatDailyCounterShardEntity::class,
        FavoritePlaylistEntity::class,
        FavoritePlaylistSongEntity::class,
        TrafficStatsBucketEntity::class,
        PlaybackQueueStateEntity::class,
        PlaybackQueueSongEntity::class,
        BiliVideoSkipRuleEntity::class,
        BiliVideoSkipIntervalEntity::class,
        BiliVideoSkipDraftEntity::class,
        CoverUrlMappingEntity::class,
        DownloadOperationEntity::class,
        DownloadBatchEntity::class,
        DownloadBatchMemberEntity::class,
        ManagedLibraryItemEntity::class,
        PlatformPlaylistCacheEntity::class,
        PlatformPlaylistCacheTrackEntity::class,
        PlatformPlaylistCacheTrackArtistEntity::class
    ],
    version = NERI_USER_DATA_FINAL_VERSION,
    exportSchema = true
)
abstract class MemiUserDataDatabase : RoomDatabase() {
    abstract fun localPlaylistDao(): LocalPlaylistDao

    abstract fun playHistoryDao(): PlayHistoryDao

    abstract fun playlistUsageDao(): PlaylistUsageDao

    abstract fun localPlaylistPlaybackDao(): LocalPlaylistPlaybackDao

    abstract fun playbackStatsDao(): PlaybackStatsDao

    abstract fun playbackStatsSnapshotDao(): PlaybackStatsSnapshotDao

    abstract fun favoritePlaylistDao(): FavoritePlaylistDao

    abstract fun trafficStatsDao(): TrafficStatsDao

    abstract fun syncMetadataDao(): SyncMetadataDao

    abstract fun playbackQueueDao(): PlaybackQueueDao

    abstract fun biliVideoSkipDao(): BiliVideoSkipDao

    abstract fun downloadOperationDao(): DownloadOperationDao
    abstract fun downloadBatchDao(): DownloadBatchDao

    abstract fun managedLibraryItemDao(): ManagedLibraryItemDao

    abstract fun coverUrlMappingDao(): CoverUrlMappingDao

    abstract fun managedDownloadArtifactDao(): ManagedDownloadArtifactDao

    abstract fun platformPlaylistCacheDao(): PlatformPlaylistCacheDao

    companion object {
        const val DATABASE_NAME = "neri_user_data.db"

        @Volatile
        private var instance: MemiUserDataDatabase? = null

        fun getInstance(context: Context): MemiUserDataDatabase {
            return instance ?: synchronized(this) {
                instance ?: create(context.applicationContext).also { database ->
                    instance = database
                }
            }
        }

        fun create(context: Context): MemiUserDataDatabase {
            checkMainProcess(context)
            val appContext = context.applicationContext
            when (val state = checkVersion(appContext)) {
                DatabaseVersionState.Missing, is DatabaseVersionState.Compatible -> Unit
                is DatabaseVersionState.NewerThanApp -> throw DatabaseOpenException(
                    state,
                    "User database version ${state.found} is newer than supported version ${state.supported}; update the app to reopen preserved data"
                )
                is DatabaseVersionState.Unreadable -> throw DatabaseOpenException(
                    state,
                    "Cannot read user database version; preserving data for retry",
                    state.cause
                )
            }
            return Room.databaseBuilder(
                appContext,
                MemiUserDataDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(*allMigrations()).build()
        }

        /** 新版本只在这里登记，漏登会让旧版本用户升级时无法打开数据库 */
        internal fun allMigrations(): Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_FINAL,
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
            MIGRATION_20_21
        )

        private fun checkMainProcess(context: Context) {
            val expectedProcess = context.applicationInfo.processName
            val currentProcess = Application.getProcessName()
            check(currentProcess == expectedProcess) {
                "Memi user database may only be opened in the main process: " +
                    "current=$currentProcess expected=$expectedProcess"
            }
        }

        val MIGRATION_1_2: Migration = PlayHistoryMigration
        val MIGRATION_2_3: Migration = PlaylistUsageMigration
        val MIGRATION_3_4: Migration = LocalPlaylistPlaybackMigration
        val MIGRATION_4_5: Migration = PlaybackStatsMigration
        val MIGRATION_5_6: Migration = FavoritePlaylistMigration
        val MIGRATION_6_7: Migration = TrafficStatsMigration
        val MIGRATION_7_8: Migration = PlaybackQueueMigration
        val MIGRATION_8_9: Migration = BiliVideoSkipMigration
        val MIGRATION_9_10: Migration = DownloadQueueMigration
        val MIGRATION_10_11: Migration = DownloadedSongCatalogMigration
        val MIGRATION_11_12: Migration = CoverUrlMappingMigration
        val MIGRATION_12_13: Migration = DownloadSnapshotMigration
        val MIGRATION_13_14: Migration = PlatformPlaylistCacheMigration
        val MIGRATION_14_15: Migration = DownloadRomanizedLyricsMigration
        val MIGRATION_15_FINAL: Migration = DownloadFinalizationMigration
        val MIGRATION_16_17: Migration = DownloadBatchMigration
        val MIGRATION_17_18: Migration = DownloadRecoveryCursorMigration
        val MIGRATION_18_19: Migration = LyricSyncPersistenceMigration
        val MIGRATION_19_20: Migration = PlaybackStatsPagedMigration
        val MIGRATION_20_21: Migration = LocalSourceProvenanceMigration

        const val FINAL_DB_VERSION = NERI_USER_DATA_FINAL_VERSION
    }
}
