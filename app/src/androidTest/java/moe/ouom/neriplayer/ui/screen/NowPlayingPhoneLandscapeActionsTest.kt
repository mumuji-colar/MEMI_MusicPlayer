package moe.ouom.neriplayer.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import moe.ouom.neriplayer.testutil.FittedTestViewport
import moe.ouom.neriplayer.testutil.assumeComposeHostAvailable
import moe.ouom.neriplayer.ui.screen.nowplaying.NowPlayingWideLayout
import moe.ouom.neriplayer.ui.screen.nowplaying.cover.NowPlayingCoverOverflowAction
import moe.ouom.neriplayer.ui.screen.nowplaying.cover.NowPlayingCoverOverflowActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 横屏播放页把辅助按钮收进了「更多」菜单，这个测试守住两件事：
 * 1. 顶栏不再铺满图标，只剩返回与更多；
 * 2. 被收起来的入口一个都没丢，仍然能从菜单里点到。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@RunWith(AndroidJUnit4::class)
class NowPlayingPhoneLandscapeActionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val clickedActions = mutableListOf<String>()

    @Before
    fun assumeDeviceUnlocked() {
        assumeComposeHostAvailable()
    }

    @Test
    fun phoneLandscapeShowsOnlyTheOverflowEntryInTheTopBar() {
        render(280.dp)
        composeRule.onNodeWithTag("nowPlayingOverflowButton").assertIsDisplayed()
        composeRule.onNodeWithTag("nowPlayingBottomControls").assertIsDisplayed()
        // 菜单默认收起，横屏页面上不会额外占用空间
        composeRule.onNodeWithTag("overflow_queue").assertDoesNotExist()
    }

    @Test
    fun phoneLandscapeOverflowMenuStillCarriesEveryRemovedAction() {
        render(280.dp)
        val expected = listOf(
            "overflow_favorite" to "favorite",
            "overflow_comment" to "comment",
            "overflow_queue" to "queue",
            "overflow_timer" to "timer",
            "overflow_volume" to "volume",
            "overflow_lyrics" to "lyrics",
            "overflow_add" to "add",
            "overflow_more" to "more"
        )
        expected.forEach { (tag, _) ->
            composeRule.onNodeWithTag("nowPlayingOverflowButton").performClick()
            composeRule.onNodeWithTag(tag).assertIsDisplayed().performClick()
        }
        composeRule.runOnIdle {
            assertEquals(expected.map { it.second }, clickedActions)
        }
    }

    @Test
    fun shortPhoneLandscapeCanScrollToThePlaybackFooter() {
        render(180.dp)
        composeRule.onNodeWithTag("nowPlayingBottomControls")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun normalPhoneLandscapePreservesTheParentSwipeDownToCloseGesture() {
        var exitRequested = false
        render(280.dp) { exitRequested = true }
        composeRule.onNodeWithTag("phonePlayer").performTouchInput {
            down(Offset(center.x, height * 0.1f))
            moveBy(Offset(0f, height * 0.3f))
            moveBy(Offset(0f, height * 0.3f))
            up()
        }
        composeRule.runOnIdle { assertTrue("下滑应交给播放页的退出手势", exitRequested) }
    }

    private fun render(height: Dp, onNavigateUp: () -> Unit = {}) {
        val actions = NowPlayingCoverOverflowActions(
            isFavorite = false,
            favoriteEnabled = true,
            commentAvailable = true,
            sleepTimerActive = false,
            lyricsAvailable = true,
            lyricsShowing = false,
            onFavorite = { clickedActions += "favorite" },
            onComment = { clickedActions += "comment" },
            onQueue = { clickedActions += "queue" },
            onSleepTimer = { clickedActions += "timer" },
            onVolume = { clickedActions += "volume" },
            onLyrics = { clickedActions += "lyrics" },
            onAddToPlaylist = { clickedActions += "add" },
            onMoreOptions = { clickedActions += "more" }
        )
        composeRule.setContent {
            MaterialTheme {
                FittedTestViewport(560.dp, height) {
                    SharedTransitionLayout {
                        val sharedTransitionScope = this
                        AnimatedContent(targetState = false, label = "phone_actions_fixture") {
                            NowPlayingWideLayout(
                                controlsAtBottom = false,
                                progressAtBottom = false,
                                topBar = { Box(Modifier.fillMaxWidth().height(56.dp)) },
                                cover = { modifier -> Box(modifier) },
                                identity = { Box(Modifier.fillMaxWidth().height(44.dp)) },
                                progress = { Box(Modifier.fillMaxWidth().height(64.dp)) },
                                controls = { Box(Modifier.fillMaxWidth().height(48.dp)) },
                                toolbar = {},
                                lyrics = { Box(Modifier.fillMaxSize()) },
                                modifier = Modifier.fillMaxSize().testTag("phonePlayer").pointerInput(Unit) {
                                    detectVerticalDragGestures { _, dragAmount ->
                                        if (dragAmount > 60) onNavigateUp()
                                    }
                                },
                                phoneLandscape = true,
                                phoneTopActions = {
                                    Box(Modifier.height(56.dp)) {
                                        NowPlayingCoverOverflowAction(
                                            buttonSize = 48.dp,
                                            iconSize = 24.dp,
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = this,
                                            actions = actions
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }
}
