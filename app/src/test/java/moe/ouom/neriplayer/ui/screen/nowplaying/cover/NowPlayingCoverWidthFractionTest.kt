package moe.ouom.neriplayer.ui.screen.nowplaying.cover

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 封面宽度占比的自动档：16:9 维持 0.60，20:9 放大到 0.74，两头都夹住。
 *
 * 这条逻辑存在的理由是「封面只跟宽度走」——同一块宽度下 16:9 和 20:9 拿到一样大的封面，
 * 长条屏多出来的高度就全变成了留白，所以必须让占比跟着长宽比走。
 */
class NowPlayingCoverWidthFractionTest {

    @Test
    fun sixteenByNineKeepsOriginalFraction() {
        assertEquals(
            NOW_PLAYING_COVER_MIN_WIDTH_FRACTION,
            resolveNowPlayingCoverWidthFraction(411.dp, 731.dp),
            0.001f
        )
    }

    @Test
    fun twentyByNineGrowsToUpperBound() {
        assertEquals(
            NOW_PLAYING_COVER_MAX_WIDTH_FRACTION,
            resolveNowPlayingCoverWidthFraction(411.dp, 914.dp),
            0.001f
        )
    }

    @Test
    fun ultraTallScreenIsClampedAtUpperBound() {
        assertEquals(
            NOW_PLAYING_COVER_MAX_WIDTH_FRACTION,
            resolveNowPlayingCoverWidthFraction(400.dp, 1200.dp),
            0.001f
        )
    }

    @Test
    fun shortScreenFallsBackToLowerBound() {
        assertEquals(
            NOW_PLAYING_COVER_MIN_WIDTH_FRACTION,
            resolveNowPlayingCoverWidthFraction(400.dp, 600.dp),
            0.001f
        )
    }

    @Test
    fun manualFractionIgnoresAspectRatio() {
        assertEquals(
            0.52f,
            resolveNowPlayingCoverWidthFraction(411.dp, 914.dp, manualFraction = 0.52f),
            0.001f
        )
    }

    @Test
    fun portraitSizeHonoursFractionButStillCapsByHeight() {
        // 宽度放开到 0.74（=296dp），可高度只剩 200dp，必须被 0.65 的上限压到 130dp，
        // 否则整列内容会被顶出屏幕
        assertEquals(
            130.dp,
            resolveNowPlayingCoverSize(
                wideLandscape = false,
                landscape = false,
                windowWidth = 400.dp,
                maxWidth = 400.dp,
                maxHeight = 200.dp,
                portraitWidthFraction = NOW_PLAYING_COVER_MAX_WIDTH_FRACTION
            )
        )
    }
}
