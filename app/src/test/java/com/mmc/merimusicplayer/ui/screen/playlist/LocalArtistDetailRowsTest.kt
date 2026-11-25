package com.mmc.merimusicplayer.ui.screen.playlist

import android.app.Application
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.core.crash.ExceptionHandler
import com.mmc.merimusicplayer.core.download.host.media.AndroidLocalMediaCovers
import com.mmc.merimusicplayer.core.download.host.media.AndroidLocalMediaDownloads
import com.mmc.merimusicplayer.data.local.media.source.CrashLogCleanup
import com.mmc.merimusicplayer.data.local.media.source.LocalMediaHostAccess
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.util.format.formatTotalDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class LocalArtistDetailRowsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        LocalMediaHostAccess.bind(
            downloads = AndroidLocalMediaDownloads,
            covers = AndroidLocalMediaCovers,
            crashLogs = CrashLogCleanup(ExceptionHandler::clearCrashLogs)
        )
    }

    @Test
    fun `header places the cover beside the title on phones and above it on tablets`() {
        var tablet by mutableStateOf(false)
        var coverUrl by mutableStateOf<String?>("https://example.invalid/artist.jpg")
        composeRule.setContent {
            MaterialTheme {
                if (tablet) {
                    LocalArtistDetailHeader(
                        title = "Local Artist",
                        coverUrl = coverUrl,
                        songCount = 3,
                        durationMs = 600_000L,
                        offlineMode = false,
                        tabletProfile = true
                    )
                } else {
                    LocalArtistDetailHeader(
                        title = "Local Artist",
                        coverUrl = coverUrl,
                        songCount = 3,
                        durationMs = 600_000L,
                        offlineMode = false
                    )
                }
            }
        }

        val summary = context.getString(
            CoreCommonR.string.local_artist_total_duration,
            formatTotalDuration(context, 600_000L),
            3
        )
        composeRule.onNodeWithText(summary).assertExists()
        var cover = composeRule.onNodeWithContentDescription("Local Artist").getUnclippedBoundsInRoot()
        var title = composeRule.onNodeWithText("Local Artist").getUnclippedBoundsInRoot()
        assertTrue(cover.right <= title.left)

        coverUrl = null
        composeRule.onNodeWithContentDescription("Local Artist").assertDoesNotExist()

        tablet = true
        coverUrl = "https://example.invalid/artist.jpg"
        cover = composeRule.onNodeWithContentDescription("Local Artist").getUnclippedBoundsInRoot()
        title = composeRule.onNodeWithText("Local Artist").getUnclippedBoundsInRoot()
        assertTrue(cover.bottom <= title.top)

        coverUrl = " "
        composeRule.onNodeWithContentDescription("Local Artist").assertDoesNotExist()
        composeRule.onNodeWithText(summary).assertExists()
    }

    @Test
    fun `song row shows index cover and download mark then a checkbox in selection mode`() {
        var song by mutableStateOf(song(id = 41L, coverUrl = "https://example.invalid/local.jpg"))
        var selectionMode by mutableStateOf(false)
        var selected by mutableStateOf(false)
        composeRule.setContent {
            MaterialTheme {
                LocalArtistSongRow(
                    index = 5,
                    song = song,
                    selectionMode = selectionMode,
                    selected = selected,
                    downloaded = true,
                    onClick = { events += "click" },
                    onLongClick = { events += "long" },
                    onToggleSelect = { events += "toggle" },
                    offlineMode = false
                )
            }
        }

        composeRule.onNodeWithText("5").assertExists()
        composeRule.onNodeWithText("Artist").assertExists()
        composeRule.onNodeWithContentDescription("Track").assertExists()
        composeRule.onNodeWithContentDescription(context.getString(CoreCommonR.string.downloaded))
            .assertExists()
        composeRule.onNodeWithText("Track").performClick()
        composeRule.onNodeWithText("Track").performTouchInput { longClick() }

        selectionMode = true
        selected = true
        composeRule.onNodeWithText("5").assertDoesNotExist()
        composeRule.onNode(isToggleable()).assertIsOn().performClick()

        song = song(id = 42L, coverUrl = null)
        composeRule.onNodeWithContentDescription("Track").assertDoesNotExist()
        song = song(id = 43L, coverUrl = " ")
        composeRule.onNodeWithContentDescription("Track").assertDoesNotExist()
        composeRule.onNodeWithText("Track").assertExists()

        assertEquals(listOf("click", "long", "toggle"), events)
    }

    private fun song(id: Long, coverUrl: String?) = SongItem(
        id = id,
        name = "Track",
        artist = "Artist",
        album = "Album",
        albumId = 0L,
        durationMs = 200_000L,
        coverUrl = coverUrl
    )
}
