package com.mmc.merimusicplayer.ui.screen.host

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.mmc.merimusicplayer.ui.screen.host/ExploreHostScreen
 * Created: 2025/8/11
 */

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.mmc.merimusicplayer.data.model.bilibili.video.VideoBasicInfo
import com.mmc.merimusicplayer.platform.bilibili.playback.resolver.buildBiliSongAlbum
import com.mmc.merimusicplayer.data.model.youtube.music.YouTubeMusicCreatorSection
import com.mmc.merimusicplayer.data.model.youtube.music.YouTubeMusicCreatorSummary
import com.mmc.merimusicplayer.core.di.AppContainer
import com.mmc.merimusicplayer.data.model.NeteaseArtistSummary
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.platform.youtube.api.transport.stableYouTubeMusicId
import com.mmc.merimusicplayer.ui.navigation.animateMainTabDetailCloseRootRevealFraction
import com.mmc.merimusicplayer.ui.navigation.clipMainTabDetailCloseRoot
import com.mmc.merimusicplayer.ui.effect.glass.AdvancedGlassSceneMotion
import com.mmc.merimusicplayer.ui.effect.glass.advancedGlassHostNavigationTransition
import com.mmc.merimusicplayer.ui.effect.glass.animateAdvancedGlassSceneMotion
import com.mmc.merimusicplayer.ui.navigation.rememberMainTabSceneRestoredEntry
import com.mmc.merimusicplayer.ui.screen.artist.NeteaseArtistDetailScreen
import com.mmc.merimusicplayer.ui.screen.artist.YouTubeMusicCreatorDetailScreen
import com.mmc.merimusicplayer.ui.screen.artist.YouTubeMusicCreatorItemsScreen
import com.mmc.merimusicplayer.ui.screen.playlist.BiliPlaylistDetailScreen
import com.mmc.merimusicplayer.ui.screen.playlist.NeteaseAlbumDetailScreen
import com.mmc.merimusicplayer.ui.screen.playlist.NeteasePlaylistDetailScreen
import com.mmc.merimusicplayer.ui.screen.playlist.YouTubeMusicPlaylistDetailScreen
import com.mmc.merimusicplayer.ui.screen.tab.explore.ExploreScreen
import com.mmc.merimusicplayer.ui.navigation.shouldSuppressRestoredMainTabHostEntry
import com.mmc.merimusicplayer.ui.viewmodel.playlist.BiliVideoItem
import com.mmc.merimusicplayer.ui.viewmodel.tab.AlbumSummary
import com.mmc.merimusicplayer.ui.viewmodel.tab.BiliPlaylist
import com.mmc.merimusicplayer.ui.viewmodel.tab.PlaylistSummary
import com.mmc.merimusicplayer.ui.viewmodel.tab.YouTubeMusicPlaylist
import com.mmc.merimusicplayer.util.media.CoverArtColorCache

// 探索页选中项
internal sealed class ExploreSelectedItem {
    data class Netease(val playlist: PlaylistSummary) : ExploreSelectedItem()
    data class NeteaseArtist(val artist: NeteaseArtistSummary) : ExploreSelectedItem()
    data class NeteaseArtistAlbum(
        val artist: NeteaseArtistSummary,
        val album: AlbumSummary
    ) : ExploreSelectedItem()
    data class Bilibili(val playlist: BiliPlaylist) : ExploreSelectedItem()
    data class YouTubeMusic(
        val playlist: YouTubeMusicPlaylist,
        val parentCreator: YouTubeMusicCreatorSummary? = null
    ) : ExploreSelectedItem()
    data class YouTubeMusicCreator(
        val creator: YouTubeMusicCreatorSummary,
        val parentCreator: YouTubeMusicCreatorSummary? = null
    ) : ExploreSelectedItem()
    data class YouTubeMusicCreatorItems(
        val creator: YouTubeMusicCreatorSummary,
        val section: YouTubeMusicCreatorSection
    ) : ExploreSelectedItem()
}

internal fun youtubeMusicCreatorDetailStateKey(
    creator: YouTubeMusicCreatorSummary
): String {
    return "youtube_music_creator_detail_${creator.browseId}"
}

private val ExploreSelectedItem?.navigationDepth: Int
    get() = when (this) {
        null -> 0
        is ExploreSelectedItem.NeteaseArtistAlbum -> 2
        is ExploreSelectedItem.YouTubeMusicCreatorItems -> 2
        is ExploreSelectedItem.YouTubeMusic -> if (parentCreator == null) 1 else 2
        is ExploreSelectedItem.YouTubeMusicCreator -> if (parentCreator == null) 1 else 2
        else -> 1
    }

internal fun resolveExploreSelectedDetailBackTarget(
    selected: ExploreSelectedItem?
): ExploreSelectedItem? {
    return when (selected) {
        is ExploreSelectedItem.NeteaseArtistAlbum -> {
            ExploreSelectedItem.NeteaseArtist(selected.artist)
        }
        is ExploreSelectedItem.YouTubeMusicCreatorItems -> {
            ExploreSelectedItem.YouTubeMusicCreator(selected.creator)
        }
        is ExploreSelectedItem.YouTubeMusic -> {
            selected.parentCreator?.let { creator ->
                ExploreSelectedItem.YouTubeMusicCreator(creator)
            }
        }
        is ExploreSelectedItem.YouTubeMusicCreator -> {
            selected.parentCreator?.let { creator ->
                ExploreSelectedItem.YouTubeMusicCreator(creator)
            }
        }
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreHostScreen(
    offlineMode: Boolean = false,
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onSongClickWithSourceRoute: (List<SongItem>, Int, String?) -> Unit = { songs, index, _ ->
        onSongClick(songs, index)
    },
    neteasePlaylistSourceRoute: (PlaylistSummary) -> String? = { null },
    onSongPlayPreservingQueue: (SongItem) -> Unit = {},
    onSongPlayNext: (SongItem) -> Unit = {},
    onSongAddToQueueEnd: (SongItem) -> Unit = {},
    onPlayParts: (VideoBasicInfo, Int, String) -> Unit = { _, _, _ -> },
    coherentFeedbackEnabled: Boolean = false,
    renderScene: @Composable (
        revealTopFraction: Float,
        contentTranslationYFraction: Float,
        contentScale: Float,
        sceneDepth: Int,
        content: @Composable () -> Unit
    ) -> Unit = { _, _, _, _, content ->
        content()
    }
) {
    var selected by remember { mutableStateOf<ExploreSelectedItem?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingNeteaseCoverWarmupJob by remember { mutableStateOf<Job?>(null) }
    var pendingNeteaseCoverWarmupToken by remember { mutableIntStateOf(0) }

    fun cancelPendingNeteaseCoverWarmup() {
        pendingNeteaseCoverWarmupToken += 1
        pendingNeteaseCoverWarmupJob?.cancel()
        pendingNeteaseCoverWarmupJob = null
    }

    fun openAfterNeteaseCoverWarmup(
        coverUrl: String?,
        item: ExploreSelectedItem
    ) {
        val token = pendingNeteaseCoverWarmupToken + 1
        pendingNeteaseCoverWarmupToken = token
        pendingNeteaseCoverWarmupJob?.cancel()
        pendingNeteaseCoverWarmupJob = scope.launch {
            CoverArtColorCache.preload(context, coverUrl, offlineMode)
            if (pendingNeteaseCoverWarmupToken == token) {
                selected = item
                pendingNeteaseCoverWarmupJob = null
            }
        }
    }

    fun openExploreSelectedItem(item: ExploreSelectedItem) {
        when (item) {
            is ExploreSelectedItem.Netease -> {
                openAfterNeteaseCoverWarmup(item.playlist.picUrl, item)
            }
            is ExploreSelectedItem.NeteaseArtistAlbum -> {
                openAfterNeteaseCoverWarmup(item.album.picUrl, item)
            }
            else -> {
                cancelPendingNeteaseCoverWarmup()
                selected = item
            }
        }
    }

    fun closeSelectedDetail() {
        cancelPendingNeteaseCoverWarmup()
        selected = resolveExploreSelectedDetailBackTarget(selected)
    }

    LaunchedEffect(offlineMode) {
        if (offlineMode) {
            cancelPendingNeteaseCoverWarmup()
            selected = null
        }
    }

    val gridStateSaver: Saver<LazyGridState, *> = LazyGridState.Saver
    val gridState = rememberSaveable(saver = gridStateSaver) {
        LazyGridState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val searchListStateSaver: Saver<LazyListState, *> = LazyListState.Saver
    val searchListState = rememberSaveable(saver = searchListStateSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val detailStateHolder = rememberSaveableStateHolder()
    val topAppBarState = rememberTopAppBarState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchScrollContextKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingGridRestoreIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingGridRestoreOffset by rememberSaveable { mutableIntStateOf(0) }
    var pendingTopAppBarHeightOffset by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var pendingTopAppBarContentOffset by rememberSaveable { mutableFloatStateOf(Float.NaN) }

    fun captureExploreScrollPosition() {
        val position = gridState.captureHostScrollPosition()
        pendingGridRestoreIndex = position.index
        pendingGridRestoreOffset = position.offset
        pendingTopAppBarHeightOffset = topAppBarState.heightOffset
        pendingTopAppBarContentOffset = topAppBarState.contentOffset
    }

    val navigationTransition = rememberHostPredictiveBackTransition(
        targetState = selected,
        backEnabled = selected != null,
        backTargetState = resolveExploreSelectedDetailBackTarget(selected),
        onBack = { closeSelectedDetail() },
        label = "explore_host_switch"
    )

    LaunchedEffect(selected, pendingGridRestoreIndex) {
        val restoreIndex = pendingGridRestoreIndex ?: return@LaunchedEffect
        if (selected != null) return@LaunchedEffect
        gridState.restoreHostScrollPosition(
            HostScrollPosition(
                index = restoreIndex,
                offset = pendingGridRestoreOffset
            )
        )
        if (!pendingTopAppBarHeightOffset.isNaN()) {
            topAppBarState.heightOffset = pendingTopAppBarHeightOffset
        }
        if (!pendingTopAppBarContentOffset.isNaN()) {
            topAppBarState.contentOffset = pendingTopAppBarContentOffset
        }
        pendingGridRestoreIndex = null
        pendingGridRestoreOffset = 0
        pendingTopAppBarHeightOffset = Float.NaN
        pendingTopAppBarContentOffset = Float.NaN
    }
    val suppressRestoredSceneEntry = rememberMainTabSceneRestoredEntry()
    val detailCloseRootRevealFraction =
        navigationTransition.animateMainTabDetailCloseRootRevealFraction(
            navigationDepth = { item -> item.navigationDepth },
            label = "explore_host_detail_close"
        )

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        navigationTransition.AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            contentKey = { item ->
                when (item) {
                    is ExploreSelectedItem.NeteaseArtist -> "netease_artist_${item.artist.id}"
                    is ExploreSelectedItem.YouTubeMusicCreator -> youtubeMusicCreatorDetailStateKey(item.creator)
                    else -> item
                }
            },
            transitionSpec = {
                if (
                    shouldSuppressRestoredMainTabHostEntry(
                        restoredEntry = suppressRestoredSceneEntry,
                        initialDepth = initialState.navigationDepth,
                        targetDepth = targetState.navigationDepth
                    )
                ) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    advancedGlassHostNavigationTransition(
                        forward = targetState.navigationDepth > initialState.navigationDepth,
                        coherentFeedbackEnabled = coherentFeedbackEnabled,
                        targetContentZIndex = targetState.navigationDepth.toFloat()
                    )
                }.using(SizeTransform(clip = true))
            }
        ) { current ->
            val suppressRestoredSceneMotion = shouldSuppressRestoredMainTabHostEntry(
                restoredEntry = suppressRestoredSceneEntry,
                initialDepth = navigationTransition.currentState.navigationDepth,
                targetDepth = navigationTransition.targetState.navigationDepth
            )
            val sceneMotion = if (suppressRestoredSceneMotion) {
                AdvancedGlassSceneMotion.None
            } else {
                navigationTransition.animateAdvancedGlassSceneMotion(
                    sceneState = current,
                    coherentFeedbackEnabled = coherentFeedbackEnabled,
                    navigationDepth = { item -> item.navigationDepth },
                    label = "explore_host_scene"
                )
            }
            renderScene(
                sceneMotion.revealTopFraction,
                sceneMotion.contentTranslationYFraction,
                sceneMotion.contentScale,
                current.navigationDepth
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (current == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipMainTabDetailCloseRoot(
                                    detailCloseRootRevealFraction
                                )
                        ) {
                            ExploreScreen(
                                gridState = gridState,
                                topAppBarState = topAppBarState,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                searchListState = searchListState,
                                searchScrollContextKey = searchScrollContextKey,
                                onSearchScrollContextKeyChange = {
                                    searchScrollContextKey = it
                                },
                                offlineMode = offlineMode,
                                onPlay = { pl ->
                                    captureExploreScrollPosition()
                                    AppContainer.launchBackgroundIo {
                                        AppContainer.playlistUsageRepo.recordOpen(
                                            id = pl.id,
                                            name = pl.name,
                                            picUrl = pl.picUrl,
                                            trackCount = pl.trackCount,
                                            source = "netease"
                                        )
                                    }
                                    openExploreSelectedItem(ExploreSelectedItem.Netease(pl))
                                },
                                onBiliPlaylistClick = { playlist ->
                                    captureExploreScrollPosition()
                                    AppContainer.launchBackgroundIo {
                                        AppContainer.playlistUsageRepo.recordOpen(
                                            id = playlist.mediaId,
                                            name = playlist.title,
                                            picUrl = playlist.coverUrl,
                                            trackCount = playlist.count,
                                            source = "bili",
                                            fid = playlist.fid,
                                            mid = playlist.mid,
                                            subtype = playlist.kind.name,
                                            subtitle = playlist.subtitle
                                        )
                                    }
                                    openExploreSelectedItem(ExploreSelectedItem.Bilibili(playlist))
                                },
                                onYouTubeMusicPlaylistClick = { pl ->
                                    captureExploreScrollPosition()
                                    AppContainer.launchBackgroundIo {
                                        AppContainer.playlistUsageRepo.recordOpen(
                                            id = stableYouTubeMusicId(
                                                pl.playlistId.ifBlank { pl.browseId }
                                            ),
                                            name = pl.title,
                                            picUrl = pl.coverUrl,
                                            trackCount = pl.trackCount,
                                            source = "youtubeMusic",
                                            browseId = pl.browseId,
                                            playlistId = pl.playlistId
                                        )
                                    }
                                    openExploreSelectedItem(ExploreSelectedItem.YouTubeMusic(pl))
                                },
                                onYouTubeCreatorClick = { creator ->
                                    captureExploreScrollPosition()
                                    openExploreSelectedItem(
                                        ExploreSelectedItem.YouTubeMusicCreator(creator)
                                    )
                                },
                                onNeteaseArtistClick = { artist ->
                                    captureExploreScrollPosition()
                                    openExploreSelectedItem(ExploreSelectedItem.NeteaseArtist(artist))
                                },
                                onSongClick = onSongClick,
                                onSongPlayPreservingQueue = onSongPlayPreservingQueue,
                                onSongPlayNext = onSongPlayNext,
                                onSongAddToQueueEnd = onSongAddToQueueEnd,
                                onPlayParts = onPlayParts
                            )
                        }
                    } else {
                        when (current) {
                            is ExploreSelectedItem.Netease -> {
                                NeteasePlaylistDetailScreen(
                                    playlist = current.playlist,
                                    onBack = ::closeSelectedDetail,
                                    onSongClick = { songs, index ->
                                        onSongClickWithSourceRoute(
                                            songs,
                                            index,
                                            neteasePlaylistSourceRoute(current.playlist)
                                        )
                                    },
                                    offlineMode = offlineMode
                                )
                            }

                            is ExploreSelectedItem.NeteaseArtist -> {
                                detailStateHolder.SaveableStateProvider(
                                    "netease_artist_${current.artist.id}"
                                ) {
                                    NeteaseArtistDetailScreen(
                                        artist = current.artist,
                                        onBack = ::closeSelectedDetail,
                                        onSongClick = onSongClick,
                                        onAlbumClick = { album ->
                                            openExploreSelectedItem(
                                                ExploreSelectedItem.NeteaseArtistAlbum(
                                                    artist = current.artist,
                                                    album = album
                                                )
                                            )
                                        },
                                        offlineMode = offlineMode
                                    )
                                }
                            }

                            is ExploreSelectedItem.NeteaseArtistAlbum -> {
                                NeteaseAlbumDetailScreen(
                                    album = current.album,
                                    onBack = ::closeSelectedDetail,
                                    onSongClick = onSongClick,
                                    offlineMode = offlineMode
                                )
                            }

                            is ExploreSelectedItem.Bilibili -> {
                                BiliPlaylistDetailScreen(
                                    playlist = current.playlist,
                                    onBack = ::closeSelectedDetail,
                                    onPlayAudio = { videos, index ->
                                        onSongClick(videos.map(BiliVideoItem::toExploreSongItem), index)
                                    },
                                    onPlayParts = onPlayParts,
                                    offlineMode = offlineMode
                                )
                            }

                            is ExploreSelectedItem.YouTubeMusic -> {
                                YouTubeMusicPlaylistDetailScreen(
                                    playlist = current.playlist,
                                    onBack = ::closeSelectedDetail,
                                    onSongClick = onSongClick,
                                    offlineMode = offlineMode
                                )
                            }

                            is ExploreSelectedItem.YouTubeMusicCreator -> {
                                detailStateHolder.SaveableStateProvider(
                                    key = youtubeMusicCreatorDetailStateKey(current.creator)
                                ) {
                                    YouTubeMusicCreatorDetailScreen(
                                        creator = current.creator,
                                        onBack = ::closeSelectedDetail,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = { playlist ->
                                            openExploreSelectedItem(
                                                ExploreSelectedItem.YouTubeMusic(
                                                    playlist = playlist.copy(
                                                        creatorName = playlist.creatorName.ifBlank {
                                                            current.creator.title
                                                        }
                                                    ),
                                                    parentCreator = current.creator
                                                )
                                            )
                                        },
                                        onCreatorClick = { creator ->
                                            openExploreSelectedItem(
                                                ExploreSelectedItem.YouTubeMusicCreator(
                                                    creator = creator,
                                                    parentCreator = current.creator
                                                )
                                            )
                                        },
                                        onSectionMoreClick = { section ->
                                            openExploreSelectedItem(
                                                ExploreSelectedItem.YouTubeMusicCreatorItems(
                                                    creator = current.creator,
                                                    section = section
                                                )
                                            )
                                        },
                                        offlineMode = offlineMode
                                    )
                                }
                            }

                            is ExploreSelectedItem.YouTubeMusicCreatorItems -> {
                                YouTubeMusicCreatorItemsScreen(
                                    section = current.section,
                                    creatorName = current.creator.title,
                                    onBack = ::closeSelectedDetail,
                                    onSongClick = onSongClick,
                                    offlineMode = offlineMode
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun BiliVideoItem.toExploreSongItem(): SongItem {
    return SongItem(
        id = id,
        name = title,
        artist = uploader,
        album = buildBiliSongAlbum(bvid = bvid),
        albumId = 0L,
        durationMs = durationSec * 1000L,
        coverUrl = coverUrl,
        channelId = "bilibili",
        audioId = id.toString()
    )
}
