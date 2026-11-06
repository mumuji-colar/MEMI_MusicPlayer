package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.utils.isRtl
import com.mocharealm.accompanist.lyrics.ui.utils.modifier.springPlacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.absoluteValue

internal data class FocusState(
    val firstIndex: Int,
    val allIndices: List<Int>,
    val activeInterludeIndex: Int?,
    val activeIntro: Boolean
)

private const val InitialPlacementSuppressionMs = 80L
private const val FocusFollowPlacementSuppressionMs = 0L
private const val FocusJumpPlacementSuppressionMs = 220L
private const val FocusedLineAlignmentCorrectionPasses = 18
private const val FocusedLineAlignmentTolerancePx = 1f
private const val FocusedLineEstimatePasses = 3
private const val VisibleScrollAnimationThresholdFraction = 0.25f

internal enum class FocusedLineScrollMode {
    Snap,
    Animate
}

internal fun resolveFocusedLineScrollMode(
    previousAutoScrollIndex: Int?,
    targetIndex: Int
): FocusedLineScrollMode {
    return if (previousAutoScrollIndex == null || targetIndex < 0) {
        FocusedLineScrollMode.Snap
    } else {
        // 首次布局可以直接对齐，后续包括大跨度 seek 都应保留平滑滚动
        FocusedLineScrollMode.Animate
    }
}

internal fun resolveFocusedLinePlacementSuppressionMs(
    previousAutoScrollIndex: Int?,
    targetIndex: Int
): Long {
    if (targetIndex < 0) {
        return 0L
    }
    if (previousAutoScrollIndex == null) {
        return InitialPlacementSuppressionMs
    }
    val focusShiftDistance = (targetIndex - previousAutoScrollIndex).absoluteValue
    return when {
        focusShiftDistance == 0 -> 0L
        focusShiftDistance == 1 -> {
            // 正常逐句播放要保留 accompanist 原本的 placement spring，
            // 否则会把 Apple Music 风格的弹性跟随压成线性平移
            FocusFollowPlacementSuppressionMs
        }
        else -> {
            // seek 后内部显隐/高度动画还在跑，提前恢复 placement spring 会把相邻歌词压到一起
            FocusJumpPlacementSuppressionMs
        }
    }
}

internal fun shouldRealignFocusedLineAfterLayout(
    previousAutoScrollIndex: Int?,
    targetIndex: Int
): Boolean {
    if (targetIndex < 0) {
        return false
    }
    if (previousAutoScrollIndex == null) {
        return true
    }
    return (targetIndex - previousAutoScrollIndex).absoluteValue > 1
}

internal fun shouldSuppressLinePlacementAnimation(
    isManualScrolling: Boolean,
    suppressPlacementAnimation: Boolean
): Boolean {
    return isManualScrolling || suppressPlacementAnimation
}

/**
 * 非当前行是否需要模糊。
 *
 * 用户手动滑离当前行后进入“浏览态”：此时整屏都保持清晰，方便看清远处的歌词，
 * 直到列表自动回到当前播放行（或用户自己滑回来）才恢复模糊。
 */
internal fun shouldBlurLine(
    useBlurEffect: Boolean,
    manualBrowsing: Boolean,
    distanceWeight: Int,
    isScrollInProgress: Boolean,
    isAutoScrolling: Boolean
): Boolean {
    if (!useBlurEffect || manualBrowsing || distanceWeight <= 0) {
        return false
    }
    // 手指拖动过程中不模糊，程序自动跟词时保留模糊
    return !isScrollInProgress || isAutoScrolling
}

/**
 * 估算把 [targetIndex] 滚到锚点位置所需的滚动距离（正数向下、负数向上）。
 *
 * LazyColumn 没有“给定 index 求绝对偏移”的公开 API，目标行不在可见区时只能按可见行的平均高度估算，
 * 后续的精确对齐会把这个误差吃掉。用它替代 `animateScrollToItem` 可以避免其内部 teleport
 * 造成的段落式跳动。
 */
internal fun estimateScrollDeltaToItem(
    targetIndex: Int,
    firstVisibleIndex: Int,
    firstVisibleOffset: Int,
    lastVisibleIndex: Int,
    lastVisibleEndOffset: Int,
    averageItemHeightPx: Float,
    anchorOffsetPx: Float
): Float {
    if (targetIndex in firstVisibleIndex..lastVisibleIndex) {
        return 0f
    }
    val safeAverage = averageItemHeightPx.coerceAtLeast(1f)
    return if (targetIndex > lastVisibleIndex) {
        (lastVisibleEndOffset - anchorOffsetPx) +
            safeAverage * (targetIndex - lastVisibleIndex - 1)
    } else {
        (firstVisibleOffset - anchorOffsetPx) -
            safeAverage * (firstVisibleIndex - targetIndex - 1)
    }
}

internal fun shouldAnimateVisibleFocusedLineScroll(
    previousAutoScrollIndex: Int?,
    targetIndex: Int
): Boolean {
    if (previousAutoScrollIndex == null || targetIndex < 0) {
        return false
    }
    return (targetIndex - previousAutoScrollIndex).absoluteValue > 1
}

internal fun resolveFocusedLineViewportDelta(
    itemOffset: Int,
    viewportStartOffset: Int,
    stableOffsetPx: Int,
    keepAliveZonePx: Float
): Float {
    return itemOffset - (viewportStartOffset + stableOffsetPx + keepAliveZonePx)
}

/**
 * 目标行还在屏内，但离目标位置超过一屏的四分之一：这种位移如果直接瞬移再让行级弹簧追，
 * 同样会出现各行不同步，需要按大跨度平滑滚动处理。
 */
internal fun isLargeVisibleFocusedLineScroll(
    scrollOffset: Float?,
    viewportHeightPx: Float,
    thresholdFraction: Float = VisibleScrollAnimationThresholdFraction
): Boolean {
    if (scrollOffset == null || viewportHeightPx <= 0f) {
        return false
    }
    return abs(scrollOffset) > viewportHeightPx * thresholdFraction
}

private suspend fun LazyListState.realignFocusedLineAfterLayout(
    targetIndex: Int,
    stableOffsetPx: Int,
    keepAliveZonePx: Float,
    correctionPasses: Int = FocusedLineAlignmentCorrectionPasses,
    tolerancePx: Float = FocusedLineAlignmentTolerancePx
) {
    repeat(correctionPasses) {
        withFrameNanos { }
        val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex } ?: return
        val delta = resolveFocusedLineViewportDelta(
            itemOffset = targetItem.offset,
            viewportStartOffset = layoutInfo.viewportStartOffset,
            stableOffsetPx = stableOffsetPx,
            keepAliveZonePx = keepAliveZonePx
        )
        if (abs(delta) <= tolerancePx) {
            return
        }
        scrollBy(delta)
    }
}

private fun resolveEstimatedScrollDurationMs(deltaPx: Float): Int {
    // 距离越远给的时间越长，避免几千像素的位移在固定时长里像瞬移
    return (abs(deltaPx) / 4f).toInt().coerceIn(220, 600)
}

/**
 * 把聚焦行平滑滚到锚点位置。
 *
 * 与 `animateScrollToItem` 不同：目标行不在可见区时不做 teleport，而是先按可见行平均高度
 * 估算整段位移，一次整屏滚动过去，再按实际布局小幅修正。整个过程列表是整体平移，
 * 不会出现“当前行先到位、远处行慢慢追”的错位。
 */
private suspend fun LazyListState.animateScrollToFocusedLine(
    targetIndex: Int,
    stableOffsetPx: Int,
    keepAliveZonePx: Float
) {
    repeat(FocusedLineEstimatePasses) { pass ->
        val layout = layoutInfo
        val target = layout.visibleItemsInfo.firstOrNull { it.index == targetIndex }
        if (target != null) {
            val delta = resolveFocusedLineViewportDelta(
                itemOffset = target.offset,
                viewportStartOffset = layout.viewportStartOffset,
                stableOffsetPx = stableOffsetPx,
                keepAliveZonePx = keepAliveZonePx
            )
            if (abs(delta) > FocusedLineAlignmentTolerancePx) {
                animateScrollBy(delta, tween(if (pass == 0) 320 else 180))
            }
            return
        }
        val first = layout.visibleItemsInfo.firstOrNull() ?: return
        val last = layout.visibleItemsInfo.lastOrNull() ?: return
        val heights = layout.visibleItemsInfo.map { it.size }.filter { it > 0 }
        val averageHeight = if (heights.isEmpty()) {
            last.size.toFloat()
        } else {
            heights.average().toFloat()
        }
        val delta = estimateScrollDeltaToItem(
            targetIndex = targetIndex,
            firstVisibleIndex = first.index,
            firstVisibleOffset = first.offset,
            lastVisibleIndex = last.index,
            lastVisibleEndOffset = last.offset + last.size,
            averageItemHeightPx = averageHeight,
            anchorOffsetPx = (layout.viewportStartOffset + stableOffsetPx + keepAliveZonePx)
        )
        if (abs(delta) < FocusedLineAlignmentTolerancePx) {
            return
        }
        animateScrollBy(delta, tween(resolveEstimatedScrollDurationMs(delta)))
    }
    // 估算误差过大时退回 LazyColumn 自己的定位
    if (layoutInfo.visibleItemsInfo.none { it.index == targetIndex }) {
        animateScrollToItem(targetIndex, (-stableOffsetPx - keepAliveZonePx).toInt())
    }
}

/**
 * A comprehensive lyrics view that supports Karaoke and Synced lyrics with advanced rendering.
 *
 * This composable handles:
 * - Scrolling and auto-scrolling to the current line
 * - Rendering karaoke lines with syllable-level timing and animations
 * - Rendering synced lines
 * - Displaying breathing dots during instrumental interludes
 * - Determining active and accompaniment lines
 *
 * @param listState The scroll state for the lazy list.
 * @param lyrics The lyrics data to display.
 * @param currentPosition A lambda returning the current playback position in milliseconds.
 * @param onLineClicked Callback when a line is clicked (seek to position).
 * @param onLinePressed Callback when a line is long-pressed (share/menu).
 * @param modifier The modifier to apply to the layout.
 * @param normalLineTextStyle The style for normal text lines.
 * @param accompanimentLineTextStyle The style for accompaniment/background vocals lines.
 * @param textColor The primary text color.
 * @param breathingDotsDefaults Styling defaults for the breathing dots.
 * @param blendMode The blend mode used for rendering text (e.g., [BlendMode.Plus] for glowing effects).
 * @param useBlurEffect Whether to apply blur effect to non-active lines.
 * @param offset The vertical padding/offset at the start and end of the list.
 * @param showDebugRectangles Debug flag to draw bounding boxes around glyphs.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun KaraokeLyricsView(
    listState: LazyListState,
    lyrics: SyncedLyrics,
    currentPosition: () -> Int,
    renderCurrentPosition: (() -> Int)? = null,
    onLineClicked: (ISyncedLine) -> Unit,
    onLinePressed: (ISyncedLine) -> Unit,
    modifier: Modifier = Modifier,
    normalLineTextStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        textMotion = TextMotion.Animated,
    ),
    accompanimentLineTextStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        textMotion = TextMotion.Animated,
    ),
    textColor: Color = Color.White,
    breathingDotsDefaults: KaraokeBreathingDotsDefaults = KaraokeBreathingDotsDefaults(),
    phoneticTextStyle: TextStyle = normalLineTextStyle.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
    ),
//    TODO: expose it
//    verticalFadeBrush: Brush = Brush.verticalGradient(
//        0f to Color.White.copy(0f),
//        0.05f to Color.White,
//        0.6f to Color.White,
//        1f to Color.White.copy(0f)
//    ),
    blendMode: BlendMode = BlendMode.Plus,
    useBlurEffect: Boolean = true,
    showTranslation: Boolean = true,
    showPhonetic: Boolean = true,
    animateViewportScroll: Boolean = false,
    // NeriPlayer 侧用这个开关在「跟随播放自动滚动」时禁掉手动拖动，默认允许拖动
    userScrollEnabled: Boolean = true,
    focusedLineScale: Float = 1f,
    unfocusedLineScale: Float = 0.98f,
    activeLineAlpha: Float = 1f,
    inactiveLineAlpha: Float = 0.4f,
    offset: Dp = 32.dp,
    keepAliveZone: Dp = 100.dp,
    bottomContentInset: Dp = 0.dp,
    blurDelta: Float = 3f,
    topFadeLength: Dp = 20.dp,
    bottomFadeLength: Dp = 100.dp,
    showDebugRectangles: Boolean = false,
    /**
     * 行级水平对齐。默认 [LyricsLineAlignment.Content] 保持原有语义
     * （普通行贴边、对唱行左右分列），传 [LyricsLineAlignment.Center] 才会居中。
     */
    lineAlignment: LyricsLineAlignment = LyricsLineAlignment.Content,
    /** 第一次点击只高亮该行，再点一次才跳转 */
    confirmSelectionBeforeSeek: Boolean = false,
    selectionHighlightColor: Color = Color.White
) {
    val density = LocalDensity.current
    val stableNormalTextStyle = remember(normalLineTextStyle) { normalLineTextStyle }
    val stableAccompanimentTextStyle =
        remember(accompanimentLineTextStyle) { accompanimentLineTextStyle }
    val stablePhoneticTextStyle = remember(phoneticTextStyle) { phoneticTextStyle }
    val stableOffset = remember(offset) { offset }
    val stableOffsetPx =
        remember(stableOffset) { with(density) { stableOffset.toPx().fastRoundToInt() } }
    val keepAliveZonePx = with(density) { keepAliveZone.toPx() }
    val stableBlendMode = remember(blendMode) { blendMode }

    val textMeasurer = rememberTextMeasurer()
    val layoutCache = remember { mutableStateMapOf<Int, List<SyllableLayout>>() }

    LaunchedEffect(
        lyrics,
        stableNormalTextStyle,
        stableAccompanimentTextStyle,
        stablePhoneticTextStyle
    ) {
        layoutCache.clear()
        withContext(Dispatchers.Default) {
            val normalStyle = stableNormalTextStyle.copy(textDirection = TextDirection.Content)
            val accompanimentStyle =
                stableAccompanimentTextStyle.copy(textDirection = TextDirection.Content)
            val phoneticStyle = stablePhoneticTextStyle.copy(textDirection = TextDirection.Content)

            val normalSpaceWidth = textMeasurer.measure(" ", normalStyle).size.width.toFloat()
            val accompanimentSpaceWidth =
                textMeasurer.measure(" ", accompanimentStyle).size.width.toFloat()

            lyrics.lines.forEachIndexed { index, line ->
                if (!isActive) return@forEachIndexed
                if (line is KaraokeLine) {
                    val style =
                        if (line is KaraokeLine.AccompanimentKaraokeLine) accompanimentStyle else normalStyle
                    val spaceWidth =
                        if (line is KaraokeLine.AccompanimentKaraokeLine) accompanimentSpaceWidth else normalSpaceWidth

                    val processedSyllables = if (line.alignment == KaraokeAlignment.End) {
                        line.syllables.dropLastWhile { it.content.isBlank() }
                    } else {
                        line.syllables
                    }

                    val layout = measureSyllablesAndDetermineAnimation(
                        syllables = processedSyllables,
                        textMeasurer = textMeasurer,
                        style = style,
                        phoneticStyle = phoneticStyle,
                        isAccompanimentLine = line is KaraokeLine.AccompanimentKaraokeLine,
                        spaceWidth = spaceWidth
                    )

                    withContext(Dispatchers.Main) {
                        layoutCache[index] = layout
                    }
                }
            }
        }
    }

    val currentPositionState = rememberUpdatedState(currentPosition)
    val timeProvider: () -> Int = { currentPositionState.value() }
    val renderCurrentPositionState = rememberUpdatedState(renderCurrentPosition)
    val renderTimeProvider: () -> Int = {
        renderCurrentPositionState.value?.invoke() ?: currentPositionState.value()
    }

    val accompanimentToMainMap = remember(lyrics.lines) {
        val map = mutableMapOf<Int, Int>()
        val mainLinesIndices = lyrics.lines.indices.filter { index ->
            val line = lyrics.lines[index]
            line !is KaraokeLine || line !is KaraokeLine.AccompanimentKaraokeLine
        }
        if (mainLinesIndices.isNotEmpty()) {
            lyrics.lines.forEachIndexed { index, line ->
                if (line is KaraokeLine && line is KaraokeLine.AccompanimentKaraokeLine) {
                    // Find the main line that is closest in time (either the one just before or just after)
                    val beforeIdx = mainLinesIndices.findLast { it <= index }
                    val afterIdx = mainLinesIndices.find { it >= index }

                    val anchorIndex = when {
                        beforeIdx != null && afterIdx != null -> {
                            val distBefore =
                                (line.start - lyrics.lines[beforeIdx].start).absoluteValue
                            val distAfter =
                                (lyrics.lines[afterIdx].start - line.start).absoluteValue
                            if (distBefore <= distAfter) beforeIdx else afterIdx
                        }

                        beforeIdx != null -> beforeIdx
                        afterIdx != null -> afterIdx
                        else -> mainLinesIndices.first()
                    }
                    map[index] = anchorIndex
                }
            }
        }
        map
    }
    val effectiveEndTimes = remember(lyrics.lines) {
        IntArray(lyrics.lines.size) { index ->
            val line = lyrics.lines[index]
            var maxEnd = line.end

            if (line is KaraokeLine.MainKaraokeLine) {
                line.accompanimentLines?.forEach { acc ->
                    if (acc.end > maxEnd) maxEnd = acc.end
                }
            }
            maxEnd
        }
    }

    val firstLine = lyrics.lines.firstOrNull()

    val haveDotsIntro by remember(firstLine) {
        derivedStateOf {
            if (firstLine == null) false
            else (firstLine.start > 5000)
        }
    }

    val currentTimeMs = currentPositionState.value()
    val lyricsFocusState = remember(
        lyrics,
        effectiveEndTimes,
        accompanimentToMainMap,
        haveDotsIntro,
        currentTimeMs
    ) {
        val activeIndex = lyrics.lines.indices.find { idx ->
            currentTimeMs >= lyrics.lines[idx].start && currentTimeMs < effectiveEndTimes[idx]
        }

        val first = if (activeIndex != null) {
            activeIndex
        } else {
            val nextIdx = lyrics.lines.indexOfFirst { it.start > currentTimeMs }
            if (nextIdx != -1) nextIdx else lyrics.lines.lastIndex
        }

        val base = lyrics.lines.indices.filter { index ->
            currentTimeMs >= lyrics.lines[index].start && currentTimeMs < effectiveEndTimes[index]
        }
        val result = base.toMutableSet()
        base.forEach { index ->
            val line = lyrics.lines.getOrNull(index)
            if (line is KaraokeLine && line is KaraokeLine.AccompanimentKaraokeLine) {
                accompanimentToMainMap[index]?.let { result.add(it) }
            }
        }

        val activeInterludeIndex = lyrics.lines.indices.find { index ->
            val line = lyrics.lines[index]
            val previousLine = lyrics.lines.getOrNull(index - 1)
            previousLine != null &&
                (line.start - previousLine.end > 5000) &&
                currentTimeMs in previousLine.end..line.start
        }
        val activeIntro = haveDotsIntro && currentTimeMs in 0 until (firstLine?.start ?: 0)

        FocusState(first, result.toList().sorted(), activeInterludeIndex, activeIntro)
    }

    val scrollInCode = remember { mutableStateOf(false) }
    // 不要挂 lyrics 做 key：歌词对象一旦重建，选中态会被悄悄清掉，高亮就会闪一下没了
    var pendingSelectionIndex by remember { mutableStateOf<Int?>(null) }
    val lastAutoScrollIndex = remember(lyrics) { mutableStateOf<Int?>(null) }
    val suppressPlacementAnimation = remember(lyrics) { mutableStateOf(true) }

    val isManualScrolling by remember {
        derivedStateOf {
            listState.isScrollInProgress && !scrollInCode.value
        }
    }
    // 用户手动滑离当前行后进入浏览态：整屏保持清晰，直到歌词自动回到当前播放行
    var manualBrowsing by remember(lyrics) { mutableStateOf(false) }
    // 程序正在自动跟词：拖动过程中不模糊，但自动滚动要保留模糊
    var autoScrollInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(isManualScrolling, lyricsFocusState.firstIndex) {
        if (isManualScrolling) {
            manualBrowsing = true
            return@LaunchedEffect
        }
        if (manualBrowsing) {
            // 松手后当前播放行仍在屏幕里，说明只是小幅微调，直接恢复模糊
            val focusVisible = listState.layoutInfo.visibleItemsInfo
                .any { it.index == lyricsFocusState.firstIndex }
            if (focusVisible) {
                manualBrowsing = false
            }
        }
    }

    LaunchedEffect(lyricsFocusState.firstIndex, stableOffsetPx, keepAliveZonePx, layoutCache.size, pendingSelectionIndex) {
        val firstIndex = lyricsFocusState.firstIndex
        // 选中待确认期间不跟随播放位置，等高亮熄灭后再回来
        if (pendingSelectionIndex != null) {
            return@LaunchedEffect
        }
        if (!scrollInCode.value && firstIndex in lyrics.lines.indices) {
            val items = listState.layoutInfo.visibleItemsInfo
            val targetItem = items.firstOrNull { it.index == firstIndex }
            val previousAutoScrollIndex = lastAutoScrollIndex.value
            val scrollMode = resolveFocusedLineScrollMode(
                previousAutoScrollIndex = previousAutoScrollIndex,
                targetIndex = firstIndex
            )
            val placementSuppressionMs = resolveFocusedLinePlacementSuppressionMs(
                previousAutoScrollIndex = previousAutoScrollIndex,
                targetIndex = firstIndex
            )
            val targetScrollOffset = (-stableOffsetPx - keepAliveZonePx).toInt()
            val scrollOffset =
                targetItem?.let {
                    resolveFocusedLineViewportDelta(
                        itemOffset = it.offset,
                        viewportStartOffset = listState.layoutInfo.viewportStartOffset,
                        stableOffsetPx = stableOffsetPx,
                        keepAliveZonePx = keepAliveZonePx
                    )
                }
            val viewportHeightPx = (
                listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                ).toFloat()
            // 目标行还在屏内、但距离目标位置超过四分之一屏时，同样属于大跨度位移
            val largeVisibleScroll = isLargeVisibleFocusedLineScroll(
                scrollOffset = scrollOffset,
                viewportHeightPx = viewportHeightPx
            )
            val shouldAnimateVisibleScroll = animateViewportScroll ||
                largeVisibleScroll ||
                shouldAnimateVisibleFocusedLineScroll(
                    previousAutoScrollIndex = previousAutoScrollIndex,
                    targetIndex = firstIndex
                )
            try {
                scrollInCode.value = true
                autoScrollInProgress = true
                // 目标行不可见或位移很大时，弹簧布局的速度差会变成“当前行先回、
                // 远处的行慢半拍”，所以整段滚动都走 snap；逐句跟随则保留弹性
                suppressPlacementAnimation.value =
                    animateViewportScroll ||
                        placementSuppressionMs > 0L ||
                        scrollOffset == null ||
                        largeVisibleScroll
                if (scrollOffset != null) {
                    if (scrollOffset != 0f) {
                        if (shouldAnimateVisibleScroll) {
                            // seek/大跳转时，即便目标行还在可见区，也要保留用户手动滚动般的平滑过渡
                            listState.animateScrollBy(scrollOffset)
                        } else {
                            // 逐句播放保留原本的直接滚动，让 placement spring 负责 Apple Music 风格弹性
                            listState.scrollBy(scrollOffset)
                        }
                    }
                } else {
                    when {
                        animateViewportScroll -> {
                            listState.animateScrollToItem(firstIndex, targetScrollOffset)
                        }
                        scrollMode == FocusedLineScrollMode.Snap -> {
                            listState.scrollToItem(firstIndex, targetScrollOffset)
                        }
                        else -> {
                            // 目标行不在可见区时不要用 animateScrollToItem：它内部的 teleport
                            // 会让整屏跳段，配合行级弹簧就出现“当前行先回、其它行慢半拍”
                            listState.animateScrollToFocusedLine(
                                targetIndex = firstIndex,
                                stableOffsetPx = stableOffsetPx,
                                keepAliveZonePx = keepAliveZonePx
                            )
                        }
                    }
                }
                lastAutoScrollIndex.value = firstIndex
                if (!animateViewportScroll &&
                    shouldRealignFocusedLineAfterLayout(previousAutoScrollIndex, firstIndex)
                ) {
                    listState.realignFocusedLineAfterLayout(
                        targetIndex = firstIndex,
                        stableOffsetPx = stableOffsetPx,
                        keepAliveZonePx = keepAliveZonePx
                    )
                }
                // 已经回到当前播放行，结束浏览态，恢复非当前行的模糊
                manualBrowsing = false
                if (!animateViewportScroll && placementSuppressionMs > 0L) {
                    delay(placementSuppressionMs)
                }
            } catch (_: Exception) {
            } finally {
                scrollInCode.value = false
                autoScrollInProgress = false
                suppressPlacementAnimation.value = false
            }
        }
    }

    // 4 秒内没有第二次点击就自动收起高亮，列表回到当前播放行
    LaunchedEffect(pendingSelectionIndex) {
        val pending = pendingSelectionIndex ?: return@LaunchedEffect
        delay(4_000L)
        if (pendingSelectionIndex == pending) {
            pendingSelectionIndex = null
        }
    }
    LookaheadScope {
        Box(modifier = modifier.clipToBounds()) {
            LazyColumn(
                    state = listState,
                    userScrollEnabled = userScrollEnabled,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                val topFade = (topFadeLength.toPx() / size.height).coerceIn(0f, 0.48f)
                                val bottomFade = (bottomFadeLength.toPx() / size.height).coerceIn(0f, 0.48f)
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        topFade to Color.Black,
                                        (1f - bottomFade).coerceIn(0f, 1f) to Color.Black,
                                        1f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        }
                        .layout { measurable, constraints ->
                            val extraHeightPx = (keepAliveZone * 2).roundToPx()

                            val placeable = measurable.measure(
                                constraints.copy(
                                    maxHeight = constraints.maxHeight + extraHeightPx
                                )
                            )

                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(0, -(keepAliveZone.roundToPx()))
                            }
                        },
                    contentPadding = PaddingValues(
                        top = stableOffset + keepAliveZone,
                        bottom = stableOffset + keepAliveZone + bottomContentInset
                    )
                ) {
                    itemsIndexed(
                        items = lyrics.lines,
                        key = { index, line -> "${line.start}-${line.end}-$index" }
                    ) { index, line ->
                        val isCurrentFocusLine = index in lyricsFocusState.allIndices
                        val isLineRtl =
                            when (line) {
                                is KaraokeLine -> {
                                    remember(line.syllables) { line.syllables.any { it.content.isRtl() } }
                                }

                                else -> false
                            }
                        // 统一的对齐起点系数，同时驱动行容器、音节坐标、缩放原点与呼吸点
                        val lineHorizontalBias = remember(lineAlignment, line, isLineRtl) {
                            when (line) {
                                is KaraokeLine -> resolveLyricsLineHorizontalBias(
                                    lineAlignment = lineAlignment,
                                    contentAlignment = line.alignment,
                                    isRtl = isLineRtl
                                )

                                is SyncedLine -> resolveLyricsLineHorizontalBias(
                                    lineAlignment = lineAlignment,
                                    isContentRightAligned = isLineRtl,
                                    isDuetLine = false
                                )

                                else -> if (isLineRtl) 1f else 0f
                            }
                        }
                        val lineColumnAlignment = remember(lineHorizontalBias) {
                            resolveColumnAlignment(lineHorizontalBias)
                        }

                        val distanceWeightState = remember(useBlurEffect, lyricsFocusState) {
                            derivedStateOf {
                                val start = lyricsFocusState.allIndices.firstOrNull() ?: lyricsFocusState.firstIndex
                                val end = lyricsFocusState.allIndices.lastOrNull() ?: lyricsFocusState.firstIndex
                                maxOf(0, start - index, index - end)
                            }
                        }

                        val dynamicStiffness by remember(distanceWeightState.value) {
                            derivedStateOf {
                                (120f - (distanceWeightState.value * 20f)).coerceAtLeast(20f)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .springPlacement(
                                    this@LookaheadScope,
                                    "${line.start}-${line.end}-$index",
                                    shouldSuppressLinePlacementAnimation(
                                        isManualScrolling = isManualScrolling,
                                        suppressPlacementAnimation = suppressPlacementAnimation.value
                                    ),
                                    stiffness = dynamicStiffness
                                ),
                            horizontalAlignment = lineColumnAlignment
                        ) {
                            val animDuration = 600

                            val previousLine = lyrics.lines.getOrNull(index - 1)

                            val showDotsInterlude = lyricsFocusState.activeInterludeIndex == index
                            val showDotsIntro = lyricsFocusState.activeIntro && index == 0

                            AnimatedVisibility(showDotsInterlude || showDotsIntro) {
                                KaraokeBreathingDots(
                                    alignment = when (val line = previousLine ?: firstLine) {
                                        is KaraokeLine -> line.alignment
                                        is SyncedLine -> if (line.content.isRtl()) KaraokeAlignment.End else KaraokeAlignment.Start
                                        else -> KaraokeAlignment.Start
                                    },
                                    startTimeMs = previousLine?.end ?: 0,
                                    endTimeMs = if (showDotsIntro) firstLine!!.start else line.start,
                                    currentTimeProvider = renderTimeProvider,
                                    defaults = breathingDotsDefaults,
                                    horizontalBias = lineHorizontalBias,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }


                            val blurRadiusState = animateFloatAsState(
                                targetValue = if (shouldBlurLine(
                                        useBlurEffect = useBlurEffect,
                                        manualBrowsing = manualBrowsing,
                                        distanceWeight = distanceWeightState.value,
                                        isScrollInProgress = listState.isScrollInProgress,
                                        isAutoScrolling = autoScrollInProgress || scrollInCode.value
                                    )
                                ) {
                                    distanceWeightState.value * blurDelta
                                } else {
                                    0f
                                },
                                animationSpec = tween(300),
                            )

                            when (line) {
                                is KaraokeLine -> {
                                    if (line is KaraokeLine.MainKaraokeLine) {
                                        LyricsLineItem(
                                            isFocused = isCurrentFocusLine,
                                            horizontalBias = lineHorizontalBias,
                                            onLineClicked = {
                                                if (!confirmSelectionBeforeSeek ||
                                                    pendingSelectionIndex == index
                                                ) {
                                                    pendingSelectionIndex = null
                                                    onLineClicked(line)
                                                } else {
                                                    pendingSelectionIndex = index
                                                }
                                            },
                                            onLinePressed = {
                                                pendingSelectionIndex = null
                                                onLinePressed(line)
                                            },
                                            blurRadius = { blurRadiusState.value },
                                            focusedScale = focusedLineScale,
                                            unfocusedScale = unfocusedLineScale,
                                            activeAlpha = activeLineAlpha,
                                            inactiveAlpha = inactiveLineAlpha,
                                            blendMode = stableBlendMode,
                                            isSelected = pendingSelectionIndex == index,
                                            selectionHighlightColor = selectionHighlightColor,
                                        ) {
                                            KaraokeLineText(
                                                line = line,
                                                currentTimeProvider = timeProvider,
                                                renderTimeProvider = renderTimeProvider,
                                                normalLineTextStyle = stableNormalTextStyle,
                                                accompanimentLineTextStyle = stableAccompanimentTextStyle,
                                                phoneticTextStyle = stablePhoneticTextStyle,
                                                activeColor = textColor,
                                                blendMode = stableBlendMode,
                                                showDebugRectangles = showDebugRectangles,
                                                showTranslation = showTranslation,
                                                showPhonetic = showPhonetic,
                                                precalculatedLayouts = layoutCache[index],
                                                lineAlignment = lineAlignment
                                            )
                                        }
                                    }
                                }

                                is SyncedLine -> {
                                    val isLineRtl = remember(line.content) { line.content.isRtl() }
                                    LyricsLineItem(
                                        isFocused = isCurrentFocusLine,
                                        horizontalBias = lineHorizontalBias,
                                        onLineClicked = {
                                            if (!confirmSelectionBeforeSeek ||
                                                pendingSelectionIndex == index
                                            ) {
                                                pendingSelectionIndex = null
                                                onLineClicked(line)
                                            } else {
                                                pendingSelectionIndex = index
                                            }
                                        },
                                        onLinePressed = {
                                            pendingSelectionIndex = null
                                            onLinePressed(line)
                                        },
                                        blurRadius = { blurRadiusState.value },
                                        focusedScale = focusedLineScale,
                                        unfocusedScale = unfocusedLineScale,
                                        activeAlpha = activeLineAlpha,
                                        inactiveAlpha = inactiveLineAlpha,
                                        blendMode = stableBlendMode,
                                        isSelected = pendingSelectionIndex == index,
                                        selectionHighlightColor = selectionHighlightColor,
                                    ) {
                                        SyncedLineText(
                                            line = line,
                                            isLineRtl = isLineRtl,
                                            textStyle = stableNormalTextStyle.copy(lineHeight = 1.2.em),
                                            textColor = textColor,
                                            showTranslation = showTranslation,
                                            horizontalBias = lineHorizontalBias
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item("BottomSpacing") {
                        Spacer(
                            modifier = Modifier.fillMaxWidth().height(2000.dp)
                        )
                    }
                }
            }
    }
}
