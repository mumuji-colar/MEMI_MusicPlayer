package com.mmc.merimusicplayer.core.download.storage.migration.access

import android.content.Context
import com.mmc.merimusicplayer.core.download.ManagedDownloadStorage
import com.mmc.merimusicplayer.core.download.storage.migration.plan.ManagedMigrationReplacementJournal
import com.mmc.merimusicplayer.core.download.storage.migration.plan.ManagedMigrationRequest
import com.mmc.merimusicplayer.core.download.storage.migration.recovery.ManagedDownloadMigrationCheckpointStore

// 界面可以读取恢复状态，检查点写入只由下载执行流程负责
class DownloadMigrationCheckpoints(context: Context) {
    private val store = ManagedDownloadMigrationCheckpointStore(context)

    fun readRequest(): ManagedMigrationRequest? = store.readRequest()
    fun readReplacementJournal(): ManagedMigrationReplacementJournal? = store.readReplacementJournal()
    fun readProgress(workId: String): ManagedDownloadStorage.MigrationProgress? = store.readProgress(workId)
}
