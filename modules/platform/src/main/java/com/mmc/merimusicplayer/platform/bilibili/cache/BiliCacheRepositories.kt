package com.mmc.merimusicplayer.platform.bilibili.cache

import android.content.Context
import com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase
import com.mmc.merimusicplayer.platform.bilibili.cache.archive.BiliArchiveCacheRepository
import com.mmc.merimusicplayer.platform.bilibili.cache.favorite.BiliFavoriteFolderCacheRepository
import java.io.File

object BiliCacheRepositories {
    fun createArchive(
        context: Context,
        database: MemiUserDataDatabase = MemiUserDataDatabase.getInstance(context.applicationContext),
        cacheDir: File = File(context.applicationContext.filesDir, "bili_archive_cache")
    ): BiliArchiveCacheRepository {
        return BiliArchiveCacheRepository(BiliArchiveCacheRoomStore(database), cacheDir)
    }

    fun createFavoriteFolder(
        context: Context,
        database: MemiUserDataDatabase = MemiUserDataDatabase.getInstance(context.applicationContext),
        cacheDir: File = File(context.applicationContext.filesDir, "bili_favorite_cache")
    ): BiliFavoriteFolderCacheRepository {
        return BiliFavoriteFolderCacheRepository(BiliFavoriteFolderCacheRoomStore(database), cacheDir)
    }
}
