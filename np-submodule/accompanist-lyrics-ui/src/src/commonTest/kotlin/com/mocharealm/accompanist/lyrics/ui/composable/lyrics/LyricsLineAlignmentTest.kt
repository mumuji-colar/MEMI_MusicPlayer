package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 歌词行对齐的起点系数。
 *
 * 这个系数是四处渲染（音节坐标、译文对齐、缩放原点、呼吸点）的唯一来源，
 * 所以它算错就意味着四处一起错位，必须锁住。
 *
 * 重点覆盖两件事：
 * 1. 默认档（Content）必须和改动前逐位一致，否则老用户升级后画面会变。
 * 2. 居中档不能把 TTML 对唱行也压到中间。
 */
class LyricsLineAlignmentTest {

    // ---- 默认档：必须与改造前的 if/else 行为完全一致 ----

    @Test
    fun contentLtrIsStartAligned() {
        assertEquals(
            0f,
            resolveLyricsLineHorizontalBias(
                lineAlignment = LyricsLineAlignment.Content,
                contentAlignment = KaraokeAlignment.Unspecified,
                isRtl = false
            ),
            0f
        )
    }

    @Test
    fun contentRtlIsEndAligned() {
        assertEquals(
            1f,
            resolveLyricsLineHorizontalBias(
                lineAlignment = LyricsLineAlignment.Content,
                contentAlignment = KaraokeAlignment.Unspecified,
                isRtl = true
            ),
            0f
        )
    }

    @Test
    fun contentDuetStartLtrStaysLeft() {
        assertEquals(
            0f,
            resolveLyricsLineHorizontalBias(
                lineAlignment = LyricsLineAlignment.Content,
                contentAlignment = KaraokeAlignment.Start,
                isRtl = false
            ),
            0f
        )
    }

    @Test
    fun contentDuetEndLtrGoesRight() {
        // 改造前：KaraokeAlignment.End -> !isRtl -> 贴右
        assertEquals(
            1f,
            resolveLyricsLineHorizontalBias(
                lineAlignment = LyricsLineAlignment.Content,
                contentAlignment = KaraokeAlignment.End,
                isRtl = false
            ),
            0f
        )
    }

    // ---- 居中档 ----

    @Test
    fun centerPutsOrdinaryLineInTheMiddle() {
        assertEquals(
            0.5f,
            resolveLyricsLineHorizontalBias(
                lineAlignment = LyricsLineAlignment.Center,
                contentAlignment = KaraokeAlignment.Unspecified,
                isRtl = false
            ),
            0f
        )
    }

    @Test
    fun centerKeepsDuetLinesOnTheirOwnSides() {
        val firstSinger = resolveLyricsLineHorizontalBias(
            lineAlignment = LyricsLineAlignment.Center,
            contentAlignment = KaraokeAlignment.Start,
            isRtl = false
        )
        val secondSinger = resolveLyricsLineHorizontalBias(
            lineAlignment = LyricsLineAlignment.Center,
            contentAlignment = KaraokeAlignment.End,
            isRtl = false
        )
        assertEquals(0f, firstSinger, 0f)
        assertEquals(1f, secondSinger, 0f)
    }

    @Test
    fun centerLeavesRtlOrdinaryLineRightAligned() {
        // 居中档下 RTL 普通行也应居中，不能被语言方向抢走
        assertEquals(
            0.5f,
            resolveLyricsLineHorizontalBias(
                lineAlignment = LyricsLineAlignment.Center,
                contentAlignment = KaraokeAlignment.Unspecified,
                isRtl = true
            ),
            0f
        )
    }

    // ---- 非 TTML 行（SyncedLine）走另一个重载 ----

    @Test
    fun syncedLineOverloadCentersWhenRequested() {
        assertEquals(
            0.5f,
            resolveLyricsLineHorizontalBias(
                lineAlignment = LyricsLineAlignment.Center,
                isContentRightAligned = false,
                isDuetLine = false
            ),
            0f
        )
    }

    @Test
    fun syncedLineOverloadKeepsRtlOnTheRightByDefault() {
        assertEquals(
            1f,
            resolveLyricsLineHorizontalBias(
                lineAlignment = LyricsLineAlignment.Content,
                isContentRightAligned = true,
                isDuetLine = false
            ),
            0f
        )
    }

    // ---- 起点系数 -> Compose 对齐枚举 ----

    @Test
    fun biasMapsToColumnAlignment() {
        assertEquals(
            androidx.compose.ui.Alignment.Start,
            resolveColumnAlignment(0f)
        )
        assertEquals(
            androidx.compose.ui.Alignment.CenterHorizontally,
            resolveColumnAlignment(0.5f)
        )
        assertEquals(
            androidx.compose.ui.Alignment.End,
            resolveColumnAlignment(1f)
        )
    }

    @Test
    fun biasMappingToleratesInterpolatedValues() {
        // 未来若引入第三档或动画插值，0.3 / 0.7 这类中间值也应有确定归属
        assertEquals(androidx.compose.ui.Alignment.Start, resolveColumnAlignment(0.2f))
        assertEquals(androidx.compose.ui.Alignment.End, resolveColumnAlignment(0.8f))
    }

    // ---- 核心几何：一行文字在画布里的起点 ----

    @Test
    fun startXLeavesLineAtLeftEdgeWhenBiasIsZero() {
        assertEquals(
            0f,
            resolveLineStartX(canvasWidth = 1000f, lineWidth = 400f, horizontalBias = 0f),
            0f
        )
    }

    @Test
    fun startXSplitsTheRemainderWhenCentered() {
        // 两侧各留 (1000-400)/2 = 300
        assertEquals(
            300f,
            resolveLineStartX(canvasWidth = 1000f, lineWidth = 400f, horizontalBias = 0.5f),
            0f
        )
    }

    @Test
    fun startXPushesLineFlushRight() {
        assertEquals(
            600f,
            resolveLineStartX(canvasWidth = 1000f, lineWidth = 400f, horizontalBias = 1f),
            0f
        )
    }

    @Test
    fun startXIsZeroForFullWidthLineRegardlessOfBias() {
        // 整行占满时没有剩余空间，三档应当重合，不能算出负数把文字推出画布
        listOf(0f, 0.5f, 1f).forEach { bias ->
            assertEquals(
                0f,
                resolveLineStartX(canvasWidth = 400f, lineWidth = 400f, horizontalBias = bias),
                0f
            )
        }
    }

    @Test
    fun startXStaysSymmetricForCenteredShortLine() {
        // 居中时左右留白必须相等，否则观感上会偏
        val canvas = 1080f
        val line = 240f
        val startX = resolveLineStartX(canvas, line, 0.5f)
        assertEquals(startX, canvas - line - startX, 0f)
    }
}
