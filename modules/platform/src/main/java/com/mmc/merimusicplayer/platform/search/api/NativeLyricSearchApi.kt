package com.mmc.merimusicplayer.platform.search.api

import com.mmc.merimusicplayer.data.model.music.SongDetails

/** 提供原平台歌词，供调用方单独判断候选来源 */
interface NativeLyricSearchApi : SearchApi {
    suspend fun getNativeSongInfo(id: String): SongDetails
}
