package com.mmc.merimusicplayer.ui.screen.tab

import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.ui.screen.tab.explore.biliCollectionSubtitle
import com.mmc.merimusicplayer.ui.screen.tab.explore.exploreClipboardMessageRes
import com.mmc.merimusicplayer.ui.screen.tab.explore.exploreSongSubtitle
import com.mmc.merimusicplayer.ui.screen.tab.explore.exploreVisibleCoverUrl
import com.mmc.merimusicplayer.ui.screen.tab.explore.youtubeCollectionSubtitle
import com.mmc.merimusicplayer.ui.util.ClipboardCopyResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExploreSearchResultRowsTest {

    @Test
    fun collectionSubtitlesPreserveOptionalDetails() {
        assertEquals("分类 · 5 项", biliCollectionSubtitle("分类", "5 项"))
        assertEquals("5 项", biliCollectionSubtitle("  ", "5 项"))
        assertEquals("频道 · 3 首", youtubeCollectionSubtitle("频道", 3, "3 首"))
        assertEquals("频道", youtubeCollectionSubtitle("频道", 0, "0 首"))
        assertEquals("", youtubeCollectionSubtitle("", 0, "0 首"))
    }

    @Test
    fun coverAndSongSubtitleOmitOnlyMissingValues() {
        assertEquals("cover", exploreVisibleCoverUrl("cover"))
        assertEquals(" cover ", exploreVisibleCoverUrl(" cover "))
        assertNull(exploreVisibleCoverUrl(null))
        assertNull(exploreVisibleCoverUrl("  "))
        assertEquals("歌手 · 专辑", exploreSongSubtitle("歌手", "专辑"))
        assertEquals("歌手", exploreSongSubtitle("歌手", " "))
        assertEquals("专辑", exploreSongSubtitle("", "专辑"))
        assertEquals("", exploreSongSubtitle("", ""))
    }

    @Test
    fun clipboardFeedbackKeepsSuccessAndFailureDistinct() {
        assertEquals(
            CoreCommonR.string.toast_copied,
            exploreClipboardMessageRes(ClipboardCopyResult.Copied(wasTruncated = false))
        )
        assertEquals(
            CoreCommonR.string.toast_copy_truncated,
            exploreClipboardMessageRes(ClipboardCopyResult.Copied(wasTruncated = true))
        )
        assertEquals(
            CoreCommonR.string.toast_copy_failed,
            exploreClipboardMessageRes(ClipboardCopyResult.TransactionTooLarge)
        )
    }
}
