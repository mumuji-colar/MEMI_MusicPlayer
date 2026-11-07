package moe.ouom.neriplayer.ui.screen.tab.settings

import moe.ouom.neriplayer.ui.screen.tab.settings.page.SettingsHubComposition
import moe.ouom.neriplayer.ui.screen.tab.settings.page.SettingsPage
import moe.ouom.neriplayer.ui.screen.tab.settings.page.SettingsTopLevelPages
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 1.12.1 曾因分类页聚合后卡片 key 撞车而整页崩溃：
 * 原子页的 key 原先是 `"${selectedPage.name}:content"`，聚合后 selectedPage 变成分类页名，
 * 同页两张卡算出同一个 key，LazyColumn 直接抛 IllegalArgumentException。
 *
 * 这两条静态断言用来守住同一个坑：
 * 1) 每个分类页内，所有卡片 key 必须唯一
 * 2) 卡片 key 不得再依赖运行时页面名
 */
class SettingsCardKeyUniquenessTest {

    private val settingsScreenSource: String by lazy {
        val candidates = listOf(
            File("src/main/java/moe/ouom/neriplayer/ui/screen/tab/settings/SettingsScreen.kt"),
            File("app/src/main/java/moe/ouom/neriplayer/ui/screen/tab/settings/SettingsScreen.kt")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("SettingsScreen.kt not found from ${File(".").absolutePath}")
        file.readText()
    }

    @Test
    fun noCardKeyDependsOnTheRuntimePageName() {
        // 聚合渲染下 selectedPage 是分类页，用它拼 key 必然撞车
        assertFalse(
            "卡片 key 不能再使用 \${selectedPage.name}，否则分类页内会重复",
            settingsScreenSource.contains("\${selectedPage.name}")
        )
    }

    @Test
    fun everyHubPageHasUniqueCardKeys() {
        val branchKeys = parseBranchKeys(settingsScreenSource)

        SettingsHubComposition.forEach { (hub, atoms) ->
            val merged = atoms.flatMap { atom ->
                branchKeys[atom] ?: error("缺少原子页分支：$atom")
            }
            val duplicates = merged.groupingBy { it }.eachCount().filterValues { it > 1 }
            assertTrue(
                "分类页 $hub 的卡片 key 重复：$duplicates",
                duplicates.isEmpty()
            )
        }
    }

    @Test
    fun everyAtomicPageHasARenderBranch() {
        val branchKeys = parseBranchKeys(settingsScreenSource)

        // 分类页由 SettingsHubComposition 动态聚合，不在这里检查；
        // 原子页必须有静态分支，否则聚合时会静默丢内容。
        val atomicPages = SettingsPage.entries.filter { it !in SettingsTopLevelPages }
        atomicPages.forEach { page ->
            assertTrue(
                "原子页 $page 在 SettingsScreen 的 pageItems 里没有渲染分支",
                branchKeys.containsKey(page)
            )
        }
    }

    /** 解析 `SettingsPage.X to { ... }` 分支内的 item key（含铺卡与裸 item 两种写法）。 */
    private fun parseBranchKeys(source: String): Map<SettingsPage, List<String>> {
        val lines = source.lines()
        val branchStart = Regex("^\\s*SettingsPage\\.([A-Za-z]+) to \\{\\s*$")
        val starts = lines.mapIndexedNotNull { index, line ->
            branchStart.find(line)?.let { index to it.groupValues[1] }
        }
        val keyPattern = Regex("(?:miuixSettingsSectionCardItem|item)\\((?:key = )?\"([^\"]*)\"")

        return starts.mapIndexed { index, (startLine, name) ->
            val endLine = starts.getOrNull(index + 1)?.first ?: lines.size
            val body = lines.subList(startLine, endLine).joinToString("\n")
            SettingsPage.valueOf(name) to keyPattern.findAll(body).map { it.groupValues[1] }.toList()
        }.toMap()
    }
}
