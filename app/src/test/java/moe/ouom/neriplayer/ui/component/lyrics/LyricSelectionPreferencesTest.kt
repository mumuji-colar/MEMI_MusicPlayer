package moe.ouom.neriplayer.ui.component.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricSelectionPreferencesTest {
    @Test
    fun `confirm off keeps the single tap seek behaviour`() {
        assertEquals(LyricTapOutcome.SeekImmediately, resolveLyricTapOutcome(false, null, 3))
        assertEquals(LyricTapOutcome.SeekImmediately, resolveLyricTapOutcome(false, 3, 3))
    }

    @Test
    fun `first tap only highlights the line`() {
        assertEquals(LyricTapOutcome.HighlightOnly, resolveLyricTapOutcome(true, null, 3))
    }

    @Test
    fun `second tap on the highlighted line commits the seek`() {
        assertEquals(LyricTapOutcome.Commit, resolveLyricTapOutcome(true, 3, 3))
    }

    @Test
    fun `tapping another line moves the highlight instead of seeking`() {
        assertEquals(LyricTapOutcome.HighlightOnly, resolveLyricTapOutcome(true, 3, 5))
    }
}
