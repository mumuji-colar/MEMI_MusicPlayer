package com.mmc.merimusicplayer.ui.screen.nowplaying

/*
 * MEMI 輕音 - 平板竖屏「上下分区」播放页的三个基础组件
 *
 * 上半屏用唱片 + 歌词，下半屏是 Coverflow 封面流加一条迷你播放条。
 * 只在平板竖屏生效，横屏仍走原来的左右分栏布局。
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.data.identity.stableKey
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.ui.haptic.HapticIconButton
import com.mmc.merimusicplayer.ui.theme.MemiDesign
import com.mmc.merimusicplayer.ui.util.rememberSongDisplayCoverUrl
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.shape.MochiRoundedRectangle

private val DiscGrooveColor = Color(0x1FFFFFFF)

/**
 * 黑胶唱片：外圈纹路 + 中心封面，播放时匀速旋转。
 *
 * 暂停时把动画协程取消，唱片停在当前角度，恢复播放从原角度继续，不会跳回 0。
 */
@Composable
internal fun NowPlayingVinylDisc(
    song: SongItem?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val coverUrl = rememberSongDisplayCoverUrl(song)
    val angle = remember { Animatable(0f) }
    LaunchedEffect(isPlaying, song?.stableKey()) {
        if (!isPlaying) return@LaunchedEffect
        angle.animateTo(
            targetValue = angle.value + 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 26_000, easing = LinearEasing)
            )
        )
    }
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            drawCircle(Color(0xFF101014), radius = radius)
            drawCircle(DiscGrooveColor, radius = radius * 0.97f, style = Stroke(1.5f))
            for (ring in 1..5) {
                drawCircle(
                    color = DiscGrooveColor,
                    radius = radius * (0.58f + ring * 0.075f),
                    style = Stroke(1f)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize(0.46f)
                .graphicsLayer { rotationZ = angle.value }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize(0.4f)
                )
            }
        }
    }
}

/**
 * Coverflow 封面流：当前项居中放大，两侧按透视旋转后退。
 *
 * 点击非居中项先滚到中间，点居中项才真正切歌，避免误触。
 */
/** 浏览态空转多久后自动复位（毫秒） */
private const val BrowseIdleTimeoutMs = 4_000L

@Composable
internal fun NowPlayingCoverflow(
    songs: List<SongItem>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    // 空队列时 coerceIn(0, -1) 会抛 IllegalArgumentException，先把起始页夹到合法下界
    val initialPage = if (songs.isEmpty()) 0 else currentIndex.coerceIn(0, songs.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { songs.size }
    )
    // 浏览态：手指拖过 Coverflow、但还没点中间那张确认播放。
    // 只认真正的拖拽（DragInteraction.Start），程序滚动到当前歌不算，
    // 否则外部切歌时整行会莫名上移一下。
    var browsing by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState) {
        pagerState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) browsing = true
        }
    }
    LaunchedEffect(currentIndex, songs.size) {
        if (currentIndex in songs.indices && pagerState.currentPage != currentIndex) {
            // 外部切歌（迷你条、自动续播）直接复位，不留在浏览态
            browsing = false
            // 切歌会让封面、歌词、背景色同时重组。直接启动横向滚动的话，
            // 动画的第一帧和这一大波重组挤在同一帧里，表现就是顿一下。
            // 先让出两帧，等重组落定再滚，动画就顺了。
            withFrameNanos { }
            withFrameNanos { }
            pagerState.animateScrollToPage(currentIndex)
        }
    }
    // 滑动后一段时间没有新的操作就自动复位：
    // 每滑到新的一页 key 变化会重启计时，点击封面时 browsing 变 false 直接取消
    LaunchedEffect(browsing, pagerState.currentPage) {
        if (!browsing) return@LaunchedEffect
        delay(BrowseIdleTimeoutMs)
        browsing = false
        // 缩回的同时把封面带回正在播放的那首。
        // 必须用独立协程：animateScrollToPage 会改 currentPage（本 effect 的 key），
        // 直接在这里 await 会被自己触发的重启取消掉，滚动会断在中途。
        if (pagerState.currentPage != currentIndex) {
            scope.launch { pagerState.animateScrollToPage(currentIndex) }
        }
    }
    // 浏览时整行上移，把底部让给歌名
    val rowShift by animateDpAsState(
        targetValue = if (browsing) 38.dp else 0.dp,
        animationSpec = tween(260),
        label = "coverflowRowShift"
    )
    BoxWithConstraints(modifier) {
        if (songs.isEmpty()) {
            // 队列还没就绪时也要占住高度，否则下半屏会整个塌陷、迷你条贴到顶部
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxSize(0.12f)
                )
            }
            return@BoxWithConstraints
        }
        // 27%：三个完整位 81%，两端各留 9.5% 给最外侧的半截。
        // 再大就会把两侧挤成一条看不见的边（30% 时只剩 26dp），
        // 尺寸和「两侧可见」在这个排布里是互斥的，27% 是两者的平衡点
        val itemWidth = maxWidth * 0.27f
        val sidePadding = (maxWidth - itemWidth) / 2
        // 封面固定正方形，否则窄页宽会被拉成竖条；高度不足时以高度为准
        val coverSize = minOf(itemWidth, maxHeight)
        // 默认的 PagerSnapDistance.atMost(1) 会让快速一扫也只翻一页，
        // 手感像被瞬间刹停。放宽到 6 页，快扫一次会顺着衰减惯性再冲几格，
        // 然后吸附到最近的封面。
        val flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(6)
        )
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = sidePadding),
            pageSpacing = 4.dp,
            flingBehavior = flingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = -rowShift)
        ) { page ->
            val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val depth = abs(offset).coerceIn(0f, 2f)
            val song = songs[page]
            // 模糊半径收到 7dp：9dp 时边缘那圈糊得有点重
            val edgeBlur = ((depth - 0.85f) * 7f).coerceIn(0f, 7f)
            // 浏览态下停在中间的那张再放大一档，带缩放动画
            val selectedBoost by animateFloatAsState(
                targetValue = if (browsing && page == pagerState.currentPage) 1.1f else 1f,
                animationSpec = tween(240),
                label = "coverflowSelectedBoost"
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(coverSize)
                        .then(
                            if (edgeBlur > 0.5f) {
                                Modifier.blur(
                                    radius = edgeBlur.dp,
                                    edgeTreatment = BlurredEdgeTreatment.Rectangle
                                )
                            } else {
                                Modifier
                            }
                        )
                        .graphicsLayer {
                            // 倾斜 24°（最外侧 48°）让透视更明显；
                            // 同时靠 scale 补偿投影损失，否则大角度会把它压成一条细线
                            rotationY = offset * 24f
                            cameraDistance = 16f * density
                            transformOrigin = TransformOrigin.Center
                            val scale = (1f - depth * 0.05f) * selectedBoost
                            scaleX = scale.coerceAtLeast(0.85f)
                            scaleY = scale.coerceAtLeast(0.85f)
                            // 按 Android 12+ 设计，连续衰减配合模糊，越远越像退到雾里
                            alpha = (1f - depth * 0.26f).coerceIn(0.4f, 1f)
                        }
                        .clickable {
                            if (page != pagerState.currentPage) {
                                // 点两侧同样是「浏览」：滚到中间后要显示歌名
                                browsing = true
                                scope.launch { pagerState.animateScrollToPage(page) }
                            } else {
                                // 先复位：整行下移、封面缩回、歌名淡出，再交给播放
                                browsing = false
                                onSelect(page)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    NowPlayingCoverflowArtwork(song)
                }
            }
        }
        // 选中封面的歌名/歌手，落在整行上移后空出来的底部
        songs.getOrNull(pagerState.currentPage)?.let { selected ->
            AnimatedVisibility(
                visible = browsing,
                enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 3 },
                exit = fadeOut(tween(140)) + slideOutVertically(tween(160)) { it / 3 },
                // 跟着封面底部走：封面居中后再减去整行上移量，歌名贴在它下面 12dp，
                // 否则固定在容器底部会和封面之间空出一大块
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (maxHeight - coverSize) / 2 + coverSize - rowShift + 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = selected.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = selected.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingCoverflowArtwork(song: SongItem) {
    val coverUrl = rememberSongDisplayCoverUrl(song)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(3.dp)
            .clip(MochiRoundedRectangle(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxSize(0.28f)
            )
        }
    }
}

/**
 * 底部迷你播放条：歌名 + 上一首 / 播放暂停 / 下一首。
 *
 * 刻意不复用导航层那套 [com.mmc.merimusicplayer.ui.component.playback.MemiMiniPlayer]：
 * 它带共享元素飞行和容器 morph，在播放页内部会和页面自身的转场打架。
 */
@Composable
internal fun NowPlayingCompactTransportBar(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = if (MemiDesign.isClean) {
        MemiDesign.tokens.miniPlayerShape
    } else {
        MochiRoundedRectangle(26.dp)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f))
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HapticIconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Outlined.SkipPrevious,
                contentDescription = stringResource(CoreCommonR.string.player_previous)
            )
        }
        HapticIconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) CoreCommonR.string.player_pause else CoreCommonR.string.player_play
                )
            )
        }
        HapticIconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Outlined.SkipNext,
                contentDescription = stringResource(CoreCommonR.string.player_next)
            )
        }
    }
}
