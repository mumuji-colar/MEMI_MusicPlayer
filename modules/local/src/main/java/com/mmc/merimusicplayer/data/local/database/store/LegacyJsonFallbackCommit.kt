package com.mmc.merimusicplayer.data.local.database.store

import androidx.room.withTransaction
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase

/**
 * 旧 JSON 清理在 Room 事务内读取 room_primary 标记后删除文件，
 * 回退快照必须和 legacy_json 标记在同一事务内提交，清理才不会删掉刚写入的恢复文件
 */
internal suspend fun MemiUserDataDatabase.commitLegacyJsonFallback(
    writeSnapshot: () -> Unit,
    markLegacyJsonPrimary: suspend () -> Unit
) {
    withTransaction {
        writeSnapshot()
        currentCoroutineContext().ensureActive()
        markLegacyJsonPrimary()
    }
}
