package moe.ouom.neriplayer.ui.screen.tab

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.PendingSettingsSearchNavigation
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.SettingsSearchScrollOwner
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.canNavigateBackFromSettingsPage
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.ensureSplitSettingsPage
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.initialSettingsPage
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.isForwardSettingsPageTransition
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.pendingSettingsNavigationForPage
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.settingsHomeSelectedPage
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.settingsSearchResultsState
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.shouldHandoffGlass
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.shouldShowActiveGlassScene
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.shouldShowSettingsDetailHeader
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.shouldUseSettingsSplitLayout
import moe.ouom.neriplayer.ui.screen.tab.settings.navigation.shouldUseTabletSettingsTransitions
import moe.ouom.neriplayer.ui.screen.tab.settings.page.SettingsPage
import moe.ouom.neriplayer.ui.screen.tab.settings.page.SettingsSearchEntry
import moe.ouom.neriplayer.ui.screen.tab.settings.page.shouldShowSettingsHomeSearchBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsNavigationPolicyTest {
    @Test
    fun `phone landscape retains phone transitions and never becomes split settings`() {
        assertFalse(shouldUseTabletSettingsTransitions(599))
        assertFalse(shouldUseSettingsSplitLayout(599, 1200.dp))
        assertTrue(shouldUseTabletSettingsTransitions(600))
        assertTrue(shouldUseTabletSettingsTransitions(840))
    }

    @Test
    fun `tablet settings use split panes only when the current window fits`() {
        assertFalse(shouldUseSettingsSplitLayout(600, 839.dp))
        assertTrue(shouldUseSettingsSplitLayout(600, 840.dp))
        assertTrue(shouldUseSettingsSplitLayout(800, 1200.dp))
        assertFalse(shouldUseSettingsSplitLayout(800, 600.dp))
    }

    @Test
    fun `account cards own their introduction while other detail pages keep their header`() {
        assertFalse(shouldShowSettingsDetailHeader(SettingsPage.Accounts))
        SettingsPage.entries.filterNot { it == SettingsPage.Accounts }.forEach { page ->
            assertTrue(shouldShowSettingsDetailHeader(page))
        }
    }

    @Test
    fun `split navigation restores the general page without replacing an open detail`() {
        assertNull(initialSettingsPage(false))
        assertEquals(SettingsPage.General, initialSettingsPage(true))
        assertNull(ensureSplitSettingsPage(false, null))
        assertEquals(SettingsPage.General, ensureSplitSettingsPage(true, null))
        assertEquals(SettingsPage.Storage, ensureSplitSettingsPage(true, SettingsPage.Storage))
    }

    @Test
    fun `pending search navigation waits for its destination`() {
        val pending = PendingSettingsSearchNavigation(SettingsPage.Storage, "cache", 4)
        assertNull(pendingSettingsNavigationForPage(SettingsPage.General, pending))
        assertNull(pendingSettingsNavigationForPage(SettingsPage.Storage, null))
        assertSame(pending, pendingSettingsNavigationForPage(SettingsPage.Storage, pending))
    }

    @Test
    fun `split detail back is active only for nested pages`() {
        assertFalse(canNavigateBackFromSettingsPage(null, false))
        assertTrue(canNavigateBackFromSettingsPage(SettingsPage.General, false))
        assertFalse(canNavigateBackFromSettingsPage(SettingsPage.General, true))
        assertTrue(canNavigateBackFromSettingsPage(SettingsPage.StorageCacheDetails, true))
    }

    @Test
    fun `home page selection highlights the hub that owns the active page`() {
        assertNull(settingsHomeSelectedPage(null))
        // 分类页自身就是主页入口
        assertEquals(
            SettingsPage.StorageDownloads,
            settingsHomeSelectedPage(SettingsPage.StorageDownloads)
        )
        // 1.12.1 之后搜索可以直接落到原子页，主页要上溯到所属分类
        assertEquals(
            SettingsPage.StorageDownloads,
            settingsHomeSelectedPage(SettingsPage.Storage)
        )
        // 三级页回落到父分类
        assertEquals(
            SettingsPage.StorageDownloads,
            settingsHomeSelectedPage(SettingsPage.StorageCacheDetails)
        )
        assertEquals(
            SettingsPage.PlaybackHub,
            settingsHomeSelectedPage(SettingsPage.UsbExclusive)
        )
        // 独立一级页保持自身
        assertEquals(SettingsPage.About, settingsHomeSelectedPage(SettingsPage.About))
    }

    @Test
    fun `home search bar only shows after the large title collapses`() {
        // 刚进设置页：标题展开，保持干净
        assertFalse(
            shouldShowSettingsHomeSearchBar(
                collapsedFraction = 0f,
                forceVisible = false,
                laidOut = true,
                canScrollForward = true
            )
        )
        // 中途还在收标题
        assertFalse(
            shouldShowSettingsHomeSearchBar(
                collapsedFraction = 0.5f,
                forceVisible = false,
                laidOut = true,
                canScrollForward = true
            )
        )
        // 标题折叠到底：滑出
        assertTrue(
            shouldShowSettingsHomeSearchBar(
                collapsedFraction = 1f,
                forceVisible = false,
                laidOut = true,
                canScrollForward = true
            )
        )
        // 首帧尚未布局：不能闪现
        assertFalse(
            shouldShowSettingsHomeSearchBar(
                collapsedFraction = 0f,
                forceVisible = false,
                laidOut = false,
                canScrollForward = false
            )
        )
        // 内容不足一屏 / 已经滑到底：标题收不起来，也必须给入口
        assertTrue(
            shouldShowSettingsHomeSearchBar(
                collapsedFraction = 0.3f,
                forceVisible = false,
                laidOut = true,
                canScrollForward = false
            )
        )
        // 搜索词非空：即使回到顶部也保留输入框
        assertTrue(
            shouldShowSettingsHomeSearchBar(
                collapsedFraction = 0f,
                forceVisible = true,
                laidOut = true,
                canScrollForward = true
            )
        )
    }

    @Test
    fun `search results state follows query changes without replacing the navigation owner`() {
        val entry = SettingsSearchEntry(
            id = "page:Storage",
            page = SettingsPage.Storage,
            title = "Storage",
            description = "Downloads and cache",
            tokens = listOf("Storage", "Downloads"),
            targetId = "page:Storage",
            order = 0
        )
        val query = mutableStateOf("")
        val results = settingsSearchResultsState(listOf(entry), query)

        assertTrue(results.value.isEmpty())
        query.value = "Storage"
        assertEquals(listOf(entry), results.value)
        query.value = "unrelated"
        assertTrue(results.value.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `search scroll owner resets for each nonblank query and ignores blank queries`() = runTest {
        val query = mutableStateOf("")
        var scrollCount = 0
        val owner = SettingsSearchScrollOwner(query) { scrollCount++ }
        val observation = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            owner.observe()
        }

        runCurrent()
        query.value = "theme"
        Snapshot.sendApplyNotifications()
        runCurrent()
        query.value = ""
        Snapshot.sendApplyNotifications()
        runCurrent()
        query.value = "lyrics"
        Snapshot.sendApplyNotifications()
        runCurrent()
        observation.cancel()

        assertEquals(2, scrollCount)
    }

    @Test
    fun `glass handoff requires both isolation and an active transition`() {
        assertFalse(shouldHandoffGlass(isolated = false, transitionRunning = false))
        assertFalse(shouldHandoffGlass(isolated = false, transitionRunning = true))
        assertFalse(shouldHandoffGlass(isolated = true, transitionRunning = false))
        assertTrue(shouldHandoffGlass(isolated = true, transitionRunning = true))
    }

    @Test
    fun `glass scene stays active for isolation or the selected page`() {
        assertTrue(shouldShowActiveGlassScene(true, SettingsPage.General, SettingsPage.Theme))
        assertTrue(shouldShowActiveGlassScene(false, SettingsPage.General, SettingsPage.General))
        assertFalse(shouldShowActiveGlassScene(false, SettingsPage.General, SettingsPage.Theme))
    }

    @Test
    fun `opening a detail page moves forward and returning moves back`() {
        assertTrue(
            isForwardSettingsPageTransition(
                SettingsPage.Playback,
                SettingsPage.UsbExclusive
            )
        )
        assertFalse(
            isForwardSettingsPageTransition(
                SettingsPage.UsbExclusive,
                SettingsPage.Playback
            )
        )
        assertTrue(
            isForwardSettingsPageTransition(
                SettingsPage.Storage,
                SettingsPage.StorageCacheDetails
            )
        )
        assertFalse(
            isForwardSettingsPageTransition(
                SettingsPage.StorageCacheDetails,
                SettingsPage.Storage
            )
        )
    }

    @Test
    fun `regular pages follow order while a missing target cannot move forward`() {
        assertTrue(isForwardSettingsPageTransition(null, SettingsPage.General))
        assertFalse(isForwardSettingsPageTransition(null, null))
        assertFalse(isForwardSettingsPageTransition(SettingsPage.General, null))
        assertTrue(isForwardSettingsPageTransition(SettingsPage.General, SettingsPage.Theme))
        assertFalse(isForwardSettingsPageTransition(SettingsPage.Theme, SettingsPage.General))
    }
}
