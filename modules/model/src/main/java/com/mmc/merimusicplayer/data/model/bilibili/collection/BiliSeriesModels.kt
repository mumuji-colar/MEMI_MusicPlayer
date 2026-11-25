package com.mmc.merimusicplayer.data.model.bilibili.collection

data class SeriesArchivePage(
    val items: List<CollectionArchiveItem>,
    val total: Int,
    val hasMore: Boolean
)
