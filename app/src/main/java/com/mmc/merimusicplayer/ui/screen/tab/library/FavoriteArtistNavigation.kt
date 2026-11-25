package com.mmc.merimusicplayer.ui.screen.tab.library

import com.mmc.merimusicplayer.data.model.BiliUploaderSummary
import com.mmc.merimusicplayer.data.model.playlist.FavoritePlaylist
import com.mmc.merimusicplayer.data.model.youtube.music.YouTubeMusicCreatorSummary
import com.mmc.merimusicplayer.data.playlist.favorite.FAVORITE_SOURCE_BILI_ARTIST
import com.mmc.merimusicplayer.data.playlist.favorite.FAVORITE_SOURCE_YOUTUBE_ARTIST

internal fun FavoritePlaylist.toFavoriteBiliUploader(): BiliUploaderSummary? {
    if (source != FAVORITE_SOURCE_BILI_ARTIST || id <= 0 || name.isBlank()) return null
    return BiliUploaderSummary(id, name, coverUrl.orEmpty())
}

internal fun FavoritePlaylist.toFavoriteYouTubeCreator(): YouTubeMusicCreatorSummary? {
    if (source != FAVORITE_SOURCE_YOUTUBE_ARTIST || name.isBlank()) return null
    val creatorId = browseId?.takeIf { it.isNotBlank() } ?: return null
    return YouTubeMusicCreatorSummary(creatorId, name, subtitle.orEmpty(), coverUrl.orEmpty())
}
