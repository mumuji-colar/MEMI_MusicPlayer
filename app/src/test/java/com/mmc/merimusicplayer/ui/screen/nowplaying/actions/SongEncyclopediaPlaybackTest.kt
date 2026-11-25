package com.mmc.merimusicplayer.ui.screen.nowplaying.actions

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmc.merimusicplayer.core.di.AppContainer
import com.mmc.merimusicplayer.core.player.queue.identity.QueueSongIdentity
import com.mmc.merimusicplayer.core.player.queue.policy.PlayerQueueEditOwner
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.encyclopedia.EncyclopediaBasicInfo
import com.mmc.merimusicplayer.data.model.encyclopedia.EncyclopediaSimilarSong
import com.mmc.merimusicplayer.data.model.encyclopedia.SongEncyclopedia
import com.mmc.merimusicplayer.data.model.encyclopedia.encyclopediaSongKey
import com.mmc.merimusicplayer.data.model.music.MusicPlatform
import com.mmc.merimusicplayer.data.model.playback.queue.PlayerQueueSnapshot
import com.mmc.merimusicplayer.platform.netease.encyclopedia.SongEncyclopediaRepository
import com.mmc.merimusicplayer.platform.netease.encyclopedia.SongEncyclopediaResult
import com.mmc.merimusicplayer.ui.viewmodel.NowPlayingViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.mmc.merimusicplayer.R as AppR

@RunWith(AndroidJUnit4::class)
class SongEncyclopediaPlaybackTest {
    @get:Rule val composeRule = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val store = ViewModelStore()
    private val playAllRequests = mutableListOf<List<SongItem>>()
    private val source = SongItem(10L, "Original", "Artist", "Album", 0L, 1_000L, null)
    private var queue = PlayerQueueSnapshot.from(
        listOf(source.copy(id = 40L), source, source, source.copy(id = 50L)), 1
    )
    private val queueOwner = PlayerQueueEditOwner(object : QueueSongIdentity {
        override fun stableKey(song: SongItem) = song.id.toString()
        override fun sameIdentity(song: SongItem, other: SongItem?) = song.id == other?.id
    })

    @Before
    fun initialize() {
        if (!AppContainer.isInitialized()) AppContainer.initialize(context)
    }

    @After
    fun clearViewModel() {
        composeRule.runOnIdle { store.clear() }
    }

    @Test
    fun `choosing one similar song does not replace the playlist`() {
        render()

        composeRule.onNodeWithText("Similar two").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertTrue(playAllRequests.isEmpty())
            assertEquals(listOf(40L, 30L, 10L, 50L), queue.playlist.map { it.id })
            assertEquals(1, queue.currentIndex)
            assertEquals("netease", queue.playlist[1].channelId)
            assertEquals("30", queue.playlist[1].audioId)
        }
    }

    @Test
    fun `explicit play all still selects the whole similar song list`() {
        render()

        composeRule.onNodeWithText(context.getString(AppR.string.encyclopedia_action_play_all))
            .performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(listOf(20L, 30L)), playAllRequests.map { songs -> songs.map { it.id } })
        }
    }

    private fun render() {
        val vm = ViewModelProvider(store, ViewModelProvider.NewInstanceFactory())[NowPlayingViewModel::class.java]
        vm.encyclopediaRepository = object : SongEncyclopediaRepository {
            override suspend fun getEncyclopedia(song: SongItem) = SongEncyclopediaResult.Success(
                SongEncyclopedia(
                    songKey = song.encyclopediaSongKey(),
                    source = MusicPlatform.CLOUD_MUSIC,
                    basicInfo = EncyclopediaBasicInfo(song.name),
                    similarSongs = listOf(
                        EncyclopediaSimilarSong(20L, "Similar one", "First artist"),
                        EncyclopediaSimilarSong(30L, "Similar two", "Second artist")
                    ),
                    fetchedAtMs = 0L
                )
            )
        }
        composeRule.setContent {
            MaterialTheme {
                SongEncyclopediaContent(
                    viewModel = vm,
                    song = source,
                    onDismiss = {},
                    onPlaySong = { song -> queue = requireNotNull(queueOwner.replaceCurrent(queue, song)).queue },
                    onPlaySongs = { songs, _ -> playAllRequests.add(songs) }
                )
            }
        }
        composeRule.waitForIdle()
    }
}
