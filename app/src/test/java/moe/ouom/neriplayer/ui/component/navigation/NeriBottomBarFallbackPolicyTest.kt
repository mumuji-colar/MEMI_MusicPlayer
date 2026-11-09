package moe.ouom.neriplayer.ui.component.navigation

import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.ui.component.playback.NeriMiniPlayerDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeriBottomBarFallbackPolicyTest {
    @Test
    fun capsuleShrinksToItsContentOnWideScreens() {
        // 4 格 × 64dp + 两侧 4dp 内缩：胶囊比屏幕窄，玻璃跟着收窄
        assertEquals(
            264.dp,
            mochiTabBarCapsuleWidth(
                availableWidth = 371.dp,
                itemWidth = 64.dp,
                itemCount = 4
            )
        )
    }

    @Test
    fun capsuleFillsAvailableWidthWhenItemsDoNotFit() {
        assertEquals(
            240.dp,
            mochiTabBarCapsuleWidth(
                availableWidth = 240.dp,
                itemWidth = 64.dp,
                itemCount = 4
            )
        )
    }

    @Test
    fun capsuleIsEmptyWithoutItems() {
        assertEquals(
            0.dp,
            mochiTabBarCapsuleWidth(
                availableWidth = 371.dp,
                itemWidth = 64.dp,
                itemCount = 0
            )
        )
    }

    @Test
    fun alignedBarFillsAvailableWidthSoItsEdgesMatchTheMiniPlayer() {
        // 360dp 屏 - 两侧 8dp = 344dp 可用；格宽均分后胶囊正好铺满
        val available = 344.dp
        val itemWidth = resolveMochiTabItemWidth(
            availableWidth = available,
            defaultItemWidth = 64.dp,
            itemCount = 4,
            fillAvailableWidth = true
        )
        assertEquals(84.dp, itemWidth)
        assertEquals(
            available,
            mochiTabBarCapsuleWidth(
                availableWidth = available,
                itemWidth = itemWidth,
                itemCount = 4
            )
        )
    }

    @Test
    fun unalignedBarKeepsTheDefaultItemWidth() {
        assertEquals(
            64.dp,
            resolveMochiTabItemWidth(
                availableWidth = 371.dp,
                defaultItemWidth = 64.dp,
                itemCount = 4,
                fillAvailableWidth = false
            )
        )
    }

    @Test
    fun alignedBarSurvivesAnEmptyItemList() {
        assertEquals(
            64.dp,
            resolveMochiTabItemWidth(
                availableWidth = 371.dp,
                defaultItemWidth = 64.dp,
                itemCount = 0,
                fillAvailableWidth = true
            )
        )
    }

    @Test
    fun alignedPaddingMatchesTheMiniPlayerSideMargin() {
        // 两条栏靠同一个常量齐边，改一边就等于两条一起改
        assertEquals(
            NeriMiniPlayerDefaults.MochiHorizontalPadding,
            MochiAlignedBarHorizontalPadding
        )
    }

    @Test
    fun newTabStyleHidesSelectionIndicatorWithoutCustomBackground() {
        assertEquals(
            0f,
            resolveBottomBarSelectionAlpha(
                hasCustomBackground = false,
                alwaysUseNewTabStyle = true
            ),
            0f
        )
    }

    @Test
    fun oldTabStyleKeepsTranslucentSelectionIndicatorWithoutCustomBackground() {
        assertEquals(
            0.72f,
            resolveBottomBarSelectionAlpha(
                hasCustomBackground = false,
                alwaysUseNewTabStyle = false
            ),
            0f
        )
    }

    @Test
    fun customBackgroundHidesSelectionIndicator() {
        assertEquals(
            0f,
            resolveBottomBarSelectionAlpha(
                hasCustomBackground = true,
                alwaysUseNewTabStyle = false
            ),
            0f
        )
    }

    @Test
    fun customBackgroundStaysTransparentWhenBlurIsNotRequested() {
        assertFalse(
            shouldUseOpaqueBottomBarFallback(
                selectAlpha = 0f,
                baseBlurRequested = false
            )
        )
    }

    @Test
    fun solidThemeOrRequestedBlurKeepsATranslucentFailureFallback() {
        assertTrue(
            shouldUseOpaqueBottomBarFallback(
                selectAlpha = 1f,
                baseBlurRequested = false
            )
        )
        assertTrue(
            shouldUseOpaqueBottomBarFallback(
                selectAlpha = 0f,
                baseBlurRequested = true
            )
        )
    }

    @Test
    fun fallbackUsesScrimSoBackgroundCanRemainVisible() {
        assertEquals(
            BOTTOM_BAR_FALLBACK_SCRIM_ALPHA,
            resolveBottomBarFallbackScrimAlpha(
                selectAlpha = 0f,
                baseBlurRequested = true
            ),
            0f
        )
        assertEquals(
            0f,
            resolveBottomBarFallbackScrimAlpha(
                selectAlpha = 0f,
                baseBlurRequested = false
            ),
            0f
        )
    }
}
