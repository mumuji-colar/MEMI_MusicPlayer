package com.mmc.merimusicplayer.ui.screen.nowplaying

/*
 * MEMI 輕音 - 平板竖屏「上下分区」播放页的三个基础组件
 *
 * 上半屏用唱片 + 歌词，下半屏是 Coverflow 封面流加一条迷你播放条。
 * 只在平板竖屏生效，横屏仍走原来的左右分栏布局。
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    LaunchedEffect(currentIndex, songs.size) {
        if (currentIndex in songs.indices && pagerState.currentPage != currentIndex) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }
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
        // 一行看到 3 个完整封面 + 两侧各半截：中心 24%、左右各 24%，
        // 剩下的约 28% 被最外两张各分一半，正好露出半张
        val itemWidth = maxWidth * 0.24f
        val sidePadding = (maxWidth - itemWidth) / 2
        // 封面固定正方形，否则窄页宽会被拉成竖条；高度不足时以高度为准
        val coverSize = minOf(itemWidth, maxHeight)
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = sidePadding),
            pageSpacing = 6.dp,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val depth = abs(offset).coerceIn(0f, 2f)
            val song = songs[page]
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(coverSize)
                        .graphicsLayer {
                            // 页宽变窄后，大角度旋转会把侧边压成一条线，
                            // 因此透视收敛到 16°，缩放差也放缓，保证两侧封面可辨认
                            rotationY = offset * 16f
                            cameraDistance = 16f * density
                            transformOrigin = TransformOrigin.Center
                            val shrink = 1f - depth * 0.10f
                            scaleX = shrink.coerceAtLeast(0.72f)
                            scaleY = shrink.coerceAtLeast(0.72f)
                            alpha = (1f - depth * 0.34f).coerceIn(0.3f, 1f)
                        }
                        .clickable {
                            if (page != pagerState.currentPage) {
                                scope.launch { pagerState.animateScrollToPage(page) }
                            } else {
                                onSelect(page)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    NowPlayingCoverflowArtwork(song)
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
