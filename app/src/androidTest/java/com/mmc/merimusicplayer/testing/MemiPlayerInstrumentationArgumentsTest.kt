package com.mmc.merimusicplayer.testing

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemiPlayerInstrumentationArgumentsTest {
    @Test
    fun additionalExclusionPreservesExistingFiltersAndOriginalArguments() {
        val original = Bundle().apply {
            putString("notClass", "example.ExternalLyricsTest")
            putString("memiplayerAdditionalExcludedClass", "example.RotationTest")
            putString("numShards", "2")
            putString("shardIndex", "1")
            putString("notAnnotation", "example.ManualTest")
        }

        val result = withAdditionalExcludedTestClass(original)

        assertEquals("example.ExternalLyricsTest,example.RotationTest", result.getString("notClass"))
        assertEquals("2", result.getString("numShards"))
        assertEquals("1", result.getString("shardIndex"))
        assertEquals("example.ManualTest", result.getString("notAnnotation"))
        assertFalse(result.containsKey("memiplayerAdditionalExcludedClass"))
        assertEquals("example.ExternalLyricsTest", original.getString("notClass"))
        assertEquals("example.RotationTest", original.getString("memiplayerAdditionalExcludedClass"))
    }

    @Test
    fun additionalExclusionWorksWithoutExistingExclusions() {
        val result = withAdditionalExcludedTestClass(Bundle().apply {
            putString("memiplayerAdditionalExcludedClass", "example.RotationTest")
        })

        assertEquals("example.RotationTest", result.getString("notClass"))
    }

    @Test
    fun duplicateExclusionIsNotAddedAgain() {
        val result = withAdditionalExcludedTestClass(Bundle().apply {
            putString("notClass", "example.ExternalLyricsTest,example.RotationTest")
            putString("memiplayerAdditionalExcludedClass", "example.RotationTest")
        })

        assertEquals("example.ExternalLyricsTest,example.RotationTest", result.getString("notClass"))
        assertFalse(result.containsKey("memiplayerAdditionalExcludedClass"))
    }

    @Test
    fun absentOrBlankAdditionalExclusionPreservesDedicatedHostSelection() {
        assertTrue(withAdditionalExcludedTestClass(null).isEmpty)
        val original = Bundle().apply { putString("class", "example.RotationTest") }
        assertEquals("example.RotationTest", withAdditionalExcludedTestClass(original).getString("class"))
        original.putString("memiplayerAdditionalExcludedClass", " ")

        val result = withAdditionalExcludedTestClass(original)

        assertEquals("example.RotationTest", result.getString("class"))
        assertFalse(result.containsKey("notClass"))
        assertFalse(result.containsKey("memiplayerAdditionalExcludedClass"))
    }
}
