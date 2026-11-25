package moe.ouom.neriplayer.ui.screen.tab.home

/*
 * MEMI 輕音 - 首页歌曲分区的完整列表页
 *
 * 首页横滑只放前 30 首（HOME_NETEASE_SONG_LIMIT），点分区标题右侧「查看全部」
 * 进入这里按 100 首重新请求。版式直接复用歌单详情页的 PlaylistModernDetailFrame
 * 与 SongRow，保证和打开其他歌单时的观感一致。
 */

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.identity.sameIdentityAs
import moe.ouom.neriplayer.data.identity.stableKey
import moe.ouom.neriplayer.data.local.media.displayCoverUrl
import moe.ouom.neriplayer.data.local.playlist.LocalPlaylistRepository
import moe.ouom.neriplayer.data.local.playlist.system.FavoritesPlaylist
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.ui.feedback.showNeriSnackbar
import moe.ouom.neriplayer.ui.haptic.HapticIconButton
import moe.ouom.neriplayer.ui.screen.playlist.FavoriteSongLookup
import moe.ouom.neriplayer.ui.screen.playlist.PlaylistModernPlaybackActions
import moe.ouom.neriplayer.ui.screen.playlist.SongRow
import moe.ouom.neriplayer.ui.screen.playlist.resolvePlaylistDetailCoverUrl
import moe.ouom.neriplayer.ui.screen.playlist.detail.PlaylistModernDetailFrame
import moe.ouom.neriplayer.ui.screen.playlist.detail.PlaylistModernDetailHeader
import moe.ouom.neriplayer.ui.screen.playlist.detail.PlaylistModernDetailSearch
import moe.ouom.neriplayer.ui.viewmodel.tab.HomeViewModel
import moe.ouom.neriplayer.ui.viewmodel.tab.NeteaseHomeSongSource

@Composable
internal fun NeteaseHomeSongListScreen(
    source: NeteaseHomeSongSource,
    onBack: () -> Unit,
    onSongClick: (List<SongItem>, Int) -> Unit,
    offlineMode: Boolean = false,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val repository = remember(context) { LocalPlaylistRepository.getInstance(context) }
    val playlists by repository.playlists.collectAsStateWithLifecycle()
    val favorites = remember(playlists, context) {
        FavoriteSongLookup(FavoritesPlaylist.firstOrNull(playlists, context)?.songs.orEmpty())
    }
    val currentSong by PlayerManager.currentSongFlow.collectAsStateWithLifecycle()
    val playing by PlayerManager.isPlayingFlow.collectAsStateWithLifecycle()
    val shuffle by PlayerManager.shuffleModeFlow.collectAsStateWithLifecycle()
    val repeat by PlayerManager.repeatModeFlow.collectAsStateWithLifecycle()
    val background by AppContainer.settingsRepo.backgroundImageUriFlow
        .collectAsStateWithLifecycle(initialValue = null)

    var songs by remember(source) { mutableStateOf<List<SongItem>>(emptyList()) }
    var loading by remember(source) { mutableStateOf(true) }
    var errorText by remember(source) { mutableStateOf<String?>(null) }
    var refreshToken by remember(source) { mutableIntStateOf(0) }

    LaunchedEffect(source, refreshToken, offlineMode) {
        // 离线时不发远程请求：这个页面可能被进程恢复重新挂载
        if (offlineMode) {
            loading = false
            errorText = null
            return@LaunchedEffect
        }
        if (songs.isEmpty()) loading = true
        errorText = null
        vm.fetchFullSongSection(source)
            .onSuccess { loaded ->
                songs = loaded
                errorText = null
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                errorText = error.message ?: error::class.java.simpleName
            }
        loading = false
    }

    // 与歌单详情页一致的搜索槽位：本页先不提供搜索，但框架需要这组状态
    var showSearch by remember { mutableStateOf(false) }
    val inputState = remember { mutableStateOf(TextFieldValue()) }
    val query = inputState.value.text
    val focusRequester = remember { FocusRequester() }
    var headerFocused by remember { mutableStateOf(false) }
    var dockedFocused by remember { mutableStateOf(false) }

    val title = stringResource(source.titleRes)
    // 分区本身没有封面，用列表里第一首有效封面充当 Hero 图，观感与歌单详情一致
    val coverUrl = remember(songs) {
        songs.firstNotNullOfOrNull { resolvePlaylistDetailCoverUrl(it.displayCoverUrl(), null) }
    }

    PlaylistModernDetailFrame(
        header = PlaylistModernDetailHeader(
            key = source,
            headerKey = "home-song-list-header:${source.name}",
            title = title,
            coverUrl = coverUrl,
            subtitle = stringResource(CoreCommonR.string.collection_track_count_format, songs.size),
            offlineMode = offlineMode,
            hasCustomBackground = background != null
        ),
        search = PlaylistModernDetailSearch(
            showSearch, { showSearch = it }, query,
            { inputState.value = TextFieldValue(it) }, inputState, focusRequester,
            false, headerFocused, { headerFocused = it }, dockedFocused, { dockedFocused = it }
        ),
        listState = listState,
        snackbarHostState = snackbar,
        onBack = onBack,
        selectionCount = null,
        onExitSelection = {},
        onLocatePlaying = null,
        normalActions = {
            HapticIconButton(
                enabled = !offlineMode && !loading,
                onClick = { refreshToken += 1 }
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(CoreCommonR.string.recommend_refresh)
                )
            }
        },
        beforeList = {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        },
        playbackActions = {
            PlaylistModernPlaybackActions(
                songCount = songs.size,
                shuffleEnabled = shuffle,
                repeatMode = repeat,
                onPlayInOrder = {
                    if (songs.isNotEmpty()) onSongClick(songs, 0)
                },
                onShufflePlay = {
                    if (songs.isNotEmpty()) {
                        PlayerManager.setShuffle(true)
                        onSongClick(songs, Random.nextInt(songs.size))
                    }
                },
                onToggleShuffle = { PlayerManager.setShuffle(!shuffle) },
                onCycleRepeatMode = { PlayerManager.cycleRepeatMode() },
                onExportToLocalPlaylist = {
                    if (songs.isNotEmpty()) {
                        val target = songs
                        scope.launch {
                            runCatching {
                                repository.createPlaylistWithSongs("$title · MEMI", target)
                            }.onSuccess {
                                snackbar.showNeriSnackbar(
                                    context.getString(CoreCommonR.string.favorite_success)
                                )
                            }.onFailure {
                                snackbar.showNeriSnackbar(
                                    context.getString(CoreCommonR.string.server_action_failed)
                                )
                            }
                        }
                    }
                }
            )
        },
        content = {
            itemsIndexed(
                songs,
                key = { _, song -> song.stableKey() },
                contentType = { _, _ -> "home_song_list_row" }
            ) { index, song ->
                val isFavorite = favorites.contains(song)
                SongRow(
                    index = index,
                    song = song,
                    isCurrentSong = currentSong?.sameIdentityAs(song) == true,
                    animatePlayingIndicator = playing,
                    isFavorite = isFavorite,
                    onFavoriteToggle = { target, favorite ->
                        scope.launch {
                            runCatching {
                                if (favorite) repository.removeFromFavorites(target)
                                else repository.addToFavorites(target)
                            }
                        }
                    },
                    showCover = true,
                    selectionMode = false,
                    selected = false,
                    onToggleSelect = {},
                    onLongPress = {},
                    onClick = { onSongClick(songs, index) },
                    onPlayNext = { PlayerManager.addToQueueNext(song) },
                    onAddToQueueEnd = { PlayerManager.addToQueueEnd(song) },
                    snackbarHostState = snackbar,
                    offlineMode = offlineMode
                )
            }
            errorText?.let { message ->
                item(key = "home-song-list-error") {
                    Text(
                        text = stringResource(CoreCommonR.string.playlist_load_failed_format, message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            }
        },
        overlays = {}
    )
}
