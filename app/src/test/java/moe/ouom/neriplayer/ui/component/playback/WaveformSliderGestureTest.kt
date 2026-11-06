package moe.ouom.neriplayer.ui.component.playback

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaveformSliderGestureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val started = mutableListOf<Float>()
    private val changed = mutableListOf<Float>()
    private var finished = 0
    private var canceled = 0

    private fun setSlider(enabled: Boolean) {
        composeRule.setContent {
            WaveformSlider(
                value = 0.1f,
                onValueChange = { changed += it },
                onValueChangeFinished = { finished++ },
                onValueChangeStarted = { started += it },
                onValueChangeCanceled = { canceled++ },
                isPlaying = false,
                enabled = enabled,
                modifier = Modifier.width(300.dp).testTag("slider")
            )
        }
    }

    @Test
    fun `tapping the bar does not seek`() {
        setSlider(enabled = true)

        composeRule.onNodeWithTag("slider").performTouchInput {
            click(Offset(width * 0.75f, centerY))
        }

        assertTrue(started.isEmpty())
        assertTrue(changed.isEmpty())
        assertEquals(0, finished)
        assertEquals(0, canceled)
    }

    @Test
    fun `disabled slider ignores taps`() {
        setSlider(enabled = false)

        composeRule.onNodeWithTag("slider").performTouchInput {
            click(Offset(width * 0.5f, centerY))
        }

        assertTrue(started.isEmpty())
        assertEquals(0, finished)
    }

    @Test
    fun `dragging moves relative to the head instead of the finger`() {
        setSlider(enabled = true)

        composeRule.onNodeWithTag("slider").performTouchInput {
            swipeRight(startX = width * 0.2f, endX = width * 0.8f)
        }

        // 起始进度是 0.1，整宽拖动 0.6 宽后应前进到约 0.7，而不是跟到触点的 0.8
        assertEquals(1, started.size)
        assertEquals(0.1f, started.single(), 0.02f)
        assertTrue(changed.isNotEmpty())
        assertTrue(changed.last() in 0.6f..0.78f)
        assertEquals(1, finished)
        assertEquals(0, canceled)
    }

    @Test
    fun `dragging backwards from the head rewinds`() {
        setSlider(enabled = true)

        composeRule.onNodeWithTag("slider").performTouchInput {
            swipeLeft(startX = width * 0.8f, endX = width * 0.2f)
        }

        assertEquals(0.1f, started.single(), 0.02f)
        assertTrue(changed.isNotEmpty())
        assertEquals(0f, changed.last(), 0.05f)
        assertEquals(1, finished)
    }

    @Test
    fun `fraction is clamped and unavailable before layout`() {
        assertEquals(0f, sliderFractionAt(-10f, 100f))
        assertEquals(1f, sliderFractionAt(150f, 100f))
        assertEquals(0.25f, sliderFractionAt(25f, 100f))
        assertNull(sliderFractionAt(10f, 0f))
    }
}
