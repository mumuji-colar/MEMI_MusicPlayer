package com.mmc.merimusicplayer.platform.bilibili.cache.archive

import com.mmc.merimusicplayer.data.model.bilibili.cache.archive.BiliArchiveContentCache

interface BiliArchiveCacheStore {
    suspend fun read(mediaId: Long, kind: String): BiliArchiveContentCache?
    suspend fun replace(cache: BiliArchiveContentCache)
    suspend fun replaceIfNewer(cache: BiliArchiveContentCache)
    suspend fun clear(mediaId: Long, kind: String)
}
