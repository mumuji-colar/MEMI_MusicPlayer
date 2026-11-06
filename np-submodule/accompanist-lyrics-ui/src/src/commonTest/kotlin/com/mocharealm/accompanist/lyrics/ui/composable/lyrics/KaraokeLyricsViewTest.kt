package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class KaraokeLyricsViewTest {

    @Test
    fun `resolveFocusedLineScrollMode snaps only on initial layout`() {
        assertEquals(
            FocusedLineScrollMode.Snap,
            resolveFocusedLineScrollMode(previousAutoScrollIndex = null, targetIndex = 6)
        )
        assertEquals(
            FocusedLineScrollMode.Animate,
            resolveFocusedLineScrollMode(previousAutoScrollIndex = 5, targetIndex = 6)
        )
        assertEquals(
            FocusedLineScrollMode.Animate,
            resolveFocusedLineScrollMode(previousAutoScrollIndex = 5, targetIndex = 42)
        )
    }

    @Test
    fun `resolveFocusedLinePlacementSuppressionMs keeps jump transitions suppressed longer`() {
        assertEquals(
            80L,
            resolveFocusedLinePlacementSuppressionMs(previousAutoScrollIndex = null, targetIndex = 6)
        )
        assertEquals(
            0L,
            resolveFocusedLinePlacementSuppressionMs(previousAutoScrollIndex = 5, targetIndex = 6)
        )
        assertEquals(
            220L,
            resolveFocusedLinePlacementSuppressionMs(previousAutoScrollIndex = 5, targetIndex = 42)
        )
        assertEquals(
            0L,
            resolveFocusedLinePlacementSuppressionMs(previousAutoScrollIndex = 6, targetIndex = 6)
        )
    }

    @Test
    fun `shouldSuppressLinePlacementAnimation only disables placement for manual scroll or explicit suppression`() {
        assertEquals(
            true,
            shouldSuppressLinePlacementAnimation(
                isManualScrolling = true,
                suppressPlacementAnimation = false
            )
        )
        assertEquals(
            true,
            shouldSuppressLinePlacementAnimation(
                isManualScrolling = false,
                suppressPlacementAnimation = true
            )
        )
        assertEquals(
            false,
            shouldSuppressLinePlacementAnimation(
                isManualScrolling = false,
                suppressPlacementAnimation = false
            )
        )
    }

    @Test
    fun `shouldAnimateVisibleFocusedLineScroll only animates large visible seek jumps`() {
        assertEquals(
            false,
            shouldAnimateVisibleFocusedLineScroll(
                previousAutoScrollIndex = null,
                targetIndex = 6
            )
        )
        assertEquals(
            false,
            shouldAnimateVisibleFocusedLineScroll(
                previousAutoScrollIndex = 5,
                targetIndex = 6
            )
        )
        assertEquals(
            true,
            shouldAnimateVisibleFocusedLineScroll(
                previousAutoScrollIndex = 5,
                targetIndex = 42
            )
        )
    }

    @Test
    fun `resolveFocusedLineViewportDelta keeps target line anchored to configured viewport offset`() {
        assertEquals(
            0f,
            resolveFocusedLineViewportDelta(
                itemOffset = 420,
                viewportStartOffset = 0,
                stableOffsetPx = 300,
                keepAliveZonePx = 120f
            ),
            0.001f
        )
        assertEquals(
            24f,
            resolveFocusedLineViewportDelta(
                itemOffset = 444,
                viewportStartOffset = 0,
                stableOffsetPx = 300,
                keepAliveZonePx = 120f
            ),
            0.001f
        )
    }

    @Test
    fun `shouldRealignFocusedLineAfterLayout skips adjacent line transitions`() {
        assertEquals(
            true,
            shouldRealignFocusedLineAfterLayout(previousAutoScrollIndex = null, targetIndex = 6)
        )
        assertEquals(
            false,
            shouldRealignFocusedLineAfterLayout(previousAutoScrollIndex = 5, targetIndex = 6)
        )
        assertEquals(
            true,
            shouldRealignFocusedLineAfterLayout(previousAutoScrollIndex = 5, targetIndex = 42)
        )
        assertEquals(
            false,
            shouldRealignFocusedLineAfterLayout(previousAutoScrollIndex = 6, targetIndex = 6)
        )
    }

    @Test
    fun `shouldBlurLine keeps manual browsing clear until playback returns`() {
        // 正常播放：非当前行模糊
        assertEquals(
            true,
            shouldBlurLine(
                useBlurEffect = true,
                manualBrowsing = false,
                distanceWeight = 3,
                isScrollInProgress = false,
                isAutoScrolling = false
            )
        )
        // 手动滑离当前行：整屏保持清晰
        assertEquals(
            false,
            shouldBlurLine(
                useBlurEffect = true,
                manualBrowsing = true,
                distanceWeight = 3,
                isScrollInProgress = false,
                isAutoScrolling = false
            )
        )
        // 手动拖动过程中也不模糊
        assertEquals(
            false,
            shouldBlurLine(
                useBlurEffect = true,
                manualBrowsing = false,
                distanceWeight = 3,
                isScrollInProgress = true,
                isAutoScrolling = false
            )
        )
        // 自动跟词滚动时保留模糊
        assertEquals(
            true,
            shouldBlurLine(
                useBlurEffect = true,
                manualBrowsing = false,
                distanceWeight = 3,
                isScrollInProgress = true,
                isAutoScrolling = true
            )
        )
        // 当前行永远不模糊
        assertEquals(
            false,
            shouldBlurLine(
                useBlurEffect = true,
                manualBrowsing = false,
                distanceWeight = 0,
                isScrollInProgress = false,
                isAutoScrolling = false
            )
        )
        // 设置里关掉模糊效果
        assertEquals(
            false,
            shouldBlurLine(
                useBlurEffect = false,
                manualBrowsing = false,
                distanceWeight = 3,
                isScrollInProgress = false,
                isAutoScrolling = false
            )
        )
    }

    @Test
    fun `isLargeVisibleFocusedLineScroll flags long visible jumps only`() {
        // 逐句跟随：几十像素的位移，保留弹簧弹性
        assertEquals(
            false,
            isLargeVisibleFocusedLineScroll(
                scrollOffset = 80f,
                viewportHeightPx = 800f
            )
        )
        // 超过四分之一屏：按大跨度处理
        assertEquals(
            true,
            isLargeVisibleFocusedLineScroll(
                scrollOffset = 420f,
                viewportHeightPx = 800f
            )
        )
        assertEquals(
            true,
            isLargeVisibleFocusedLineScroll(
                scrollOffset = -420f,
                viewportHeightPx = 800f
            )
        )
        // 目标行不可见时由调用方走整屏滚动分支
        assertEquals(
            false,
            isLargeVisibleFocusedLineScroll(
                scrollOffset = null,
                viewportHeightPx = 800f
            )
        )
        // 首帧视口还没测量出来
        assertEquals(
            false,
            isLargeVisibleFocusedLineScroll(
                scrollOffset = 420f,
                viewportHeightPx = 0f
            )
        )
    }

    @Test
    fun `estimateScrollDeltaToItem anchors offscreen target to viewport offset`() {
        val averageItemHeight = 150f
        val anchor = 200f

        // 目标就在可见区里，不需要滚动
        assertEquals(
            0f,
            estimateScrollDeltaToItem(
                targetIndex = 12,
                firstVisibleIndex = 10,
                firstVisibleOffset = -100,
                lastVisibleIndex = 14,
                lastVisibleEndOffset = 900,
                averageItemHeightPx = averageItemHeight,
                anchorOffsetPx = anchor
            ),
            0.001f
        )
        // 向下：最后一行底部离锚点的距离 + 中间隔着的行高
        assertEquals(
            1000f,
            estimateScrollDeltaToItem(
                targetIndex = 17,
                firstVisibleIndex = 10,
                firstVisibleOffset = -100,
                lastVisibleIndex = 14,
                lastVisibleEndOffset = 900,
                averageItemHeightPx = averageItemHeight,
                anchorOffsetPx = anchor
            ),
            0.001f
        )
        // 紧邻下一行时只有自身到锚点的距离
        assertEquals(
            700f,
            estimateScrollDeltaToItem(
                targetIndex = 15,
                firstVisibleIndex = 10,
                firstVisibleOffset = -100,
                lastVisibleIndex = 14,
                lastVisibleEndOffset = 900,
                averageItemHeightPx = averageItemHeight,
                anchorOffsetPx = anchor
            ),
            0.001f
        )
        // 向上：第一行顶部离锚点的距离 + 中间隔着的行高，方向为负
        assertEquals(
            -460f,
            estimateScrollDeltaToItem(
                targetIndex = 7,
                firstVisibleIndex = 10,
                firstVisibleOffset = 40,
                lastVisibleIndex = 14,
                lastVisibleEndOffset = 900,
                averageItemHeightPx = averageItemHeight,
                anchorOffsetPx = anchor
            ),
            0.001f
        )
    }
}
