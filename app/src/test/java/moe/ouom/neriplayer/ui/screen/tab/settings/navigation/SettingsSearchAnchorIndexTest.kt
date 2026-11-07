package moe.ouom.neriplayer.ui.screen.tab.settings.navigation

import moe.ouom.neriplayer.ui.screen.tab.settings.page.SettingsPage
import moe.ouom.neriplayer.ui.screen.tab.settings.page.settingsSearchScrollAnchor
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSearchAnchorIndexTest {
    @Test
    fun cleanThemeSearchReachesTheSameColorSpecCardAfterRemovingTheHero() {
        // 1.12.2：色彩空间并入色彩风格卡后，主题在外观页共 3 张卡，色彩空间落在第 3 张
        val anchor = settingsSearchScrollAnchor(SettingsPage.Appearance, "manual:theme_color_spec")

        assertEquals(3, settingsDetailListIndex(anchor.itemIndex, SettingsPage.Appearance, clean = false))
        assertEquals(2, settingsDetailListIndex(anchor.itemIndex, SettingsPage.Appearance, clean = true))
    }

    @Test
    fun backgroundSearchReachesTheBackgroundCardInsideAppearance() {
        // 背景是「外观」分类页里主题 3 张卡之后的第 4 张
        val anchor = settingsSearchScrollAnchor(SettingsPage.Appearance, "setting:background_image_uri")

        assertEquals(4, settingsDetailListIndex(anchor.itemIndex, SettingsPage.Appearance, clean = false))
        assertEquals(3, settingsDetailListIndex(anchor.itemIndex, SettingsPage.Appearance, clean = true))
    }

    @Test
    fun pageSearchNeverProducesANegativeListIndex() {
        val anchor = settingsSearchScrollAnchor(SettingsPage.Appearance, "page:Appearance")

        assertEquals(0, settingsDetailListIndex(anchor.itemIndex, SettingsPage.Appearance, clean = false))
        assertEquals(0, settingsDetailListIndex(anchor.itemIndex, SettingsPage.Appearance, clean = true))
    }

    @Test
    fun accountHubKeepsAStableHeaderAnchorInBothStyles() {
        // 账号页并入「账号与音源」分类页后开始渲染页头，页面级入口仍落在页首
        val anchor = settingsSearchScrollAnchor(SettingsPage.AccountSources, "page:AccountSources")

        assertEquals(0, settingsDetailListIndex(anchor.itemIndex, SettingsPage.AccountSources, clean = false))
        assertEquals(0, settingsDetailListIndex(anchor.itemIndex, SettingsPage.AccountSources, clean = true))
        // 分类页有页头，clean 风格下卡片整体前移一格
        assertEquals(1, settingsDetailListIndex(2, SettingsPage.AccountSources, clean = true))
    }
}
