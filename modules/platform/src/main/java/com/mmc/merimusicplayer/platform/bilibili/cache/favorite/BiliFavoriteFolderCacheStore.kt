package com.mmc.merimusicplayer.platform.bilibili.cache.favorite

import com.mmc.merimusicplayer.data.model.bilibili.cache.favorite.BiliFavoriteFolderContentCache

interface BiliFavoriteFolderCacheStore {
    suspend fun read(mediaId: Long): BiliFavoriteFolderContentCache?
    suspend fun replace(cache: BiliFavoriteFolderContentCache)
    suspend fun replaceIfNewer(cache: BiliFavoriteFolderContentCache)
    suspend fun clear(mediaId: Long)
}
