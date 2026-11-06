package moe.ouom.neriplayer.activity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivitySystemBarPolicyTest {

    @Test
    fun `normal landscape tablets hide only the navigation bar`() {
        assertTrue(shouldHideTabletNavigationBar(600, isLandscape = true, safeModeActive = false))
        assertTrue(shouldHideTabletNavigationBar(800, isLandscape = true, safeModeActive = false))
        assertFalse(shouldHideTabletNavigationBar(599, isLandscape = true, safeModeActive = false))
        assertFalse(shouldHideTabletNavigationBar(360, isLandscape = true, safeModeActive = false))
    }

    @Test
    fun `portrait and safe mode always retain navigation`() {
        for (width in listOf(360, 599, 600, 800)) {
            assertFalse(shouldHideTabletNavigationBar(width, isLandscape = false, safeModeActive = false))
            assertFalse(shouldHideTabletNavigationBar(width, isLandscape = false, safeModeActive = true))
            assertFalse(shouldHideTabletNavigationBar(width, isLandscape = true, safeModeActive = true))
        }
    }

    @Test
    fun `phone landscape player hides the status bar`() {
        assertTrue(
            shouldHideNowPlayingStatusBar(
                playerOpen = true, isLandscape = true,
                smallestScreenWidthDp = 360, safeModeActive = false
            )
        )
        assertTrue(
            shouldHideNowPlayingStatusBar(
                playerOpen = true, isLandscape = true,
                smallestScreenWidthDp = 599, safeModeActive = false
            )
        )
    }

    @Test
    fun `status bar stays visible outside the phone landscape player`() {
        // 竖屏：状态栏正好压在挖孔那一侧，隐藏反而会让内容顶到挖孔下面
        assertFalse(
            shouldHideNowPlayingStatusBar(
                playerOpen = true, isLandscape = false,
                smallestScreenWidthDp = 360, safeModeActive = false
            )
        )
        // 没打开播放页
        assertFalse(
            shouldHideNowPlayingStatusBar(
                playerOpen = false, isLandscape = true,
                smallestScreenWidthDp = 360, safeModeActive = false
            )
        )
        // 平板横屏保持既有策略，只隐藏导航栏
        assertFalse(
            shouldHideNowPlayingStatusBar(
                playerOpen = true, isLandscape = true,
                smallestScreenWidthDp = 600, safeModeActive = false
            )
        )
        // 安全模式不碰系统栏
        assertFalse(
            shouldHideNowPlayingStatusBar(
                playerOpen = true, isLandscape = true,
                smallestScreenWidthDp = 360, safeModeActive = true
            )
        )
    }

    @Test
    fun `now playing uses light system bar icons in a light app theme`() {
        assertTrue(
            shouldUseLightSystemBarIcons(
                isDarkTheme = false,
                isNowPlayingVisible = true
            )
        )
    }

    @Test
    fun `non player screens continue to follow the app theme`() {
        assertFalse(
            shouldUseLightSystemBarIcons(
                isDarkTheme = false,
                isNowPlayingVisible = false
            )
        )
        assertTrue(
            shouldUseLightSystemBarIcons(
                isDarkTheme = true,
                isNowPlayingVisible = false
            )
        )
    }
}
