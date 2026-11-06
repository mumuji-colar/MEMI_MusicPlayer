/*
 * MEMI輕音 / MemiPlayer - A modified build of NeriPlayer.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * Copyright (C) 2026 MMC
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.data.settings.appearance/PerformanceModeTest
 */

package moe.ouom.neriplayer.data.settings.appearance

import moe.ouom.neriplayer.data.model.settings.appearance.PerformanceMode

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceModeTest {
    @Test
    fun missingPreferenceDefaultsToBalanced() {
        assertEquals(
            PerformanceMode.Balanced,
            PerformanceModePreference.resolve(value = null)
        )
    }

    @Test
    fun savedPreferenceOverridesDefaultAndNormalizesInvalidValues() {
        assertEquals(
            PerformanceMode.Saving,
            PerformanceModePreference.resolve(value = "saving")
        )
        assertEquals(
            PerformanceMode.Quality,
            PerformanceModePreference.resolve(value = "QUALITY")
        )
        assertEquals(
            PerformanceMode.Balanced,
            PerformanceModePreference.resolve(value = "unsupported")
        )
    }

    @Test
    fun normalizeReturnsCanonicalStorageValues() {
        assertEquals("saving", PerformanceModePreference.normalize("SAVING"))
        assertEquals("balanced", PerformanceModePreference.normalize("balanced"))
        assertEquals("quality", PerformanceModePreference.normalize("quality"))
        assertEquals("balanced", PerformanceModePreference.normalize("bogus"))
    }

    @Test
    fun storageValuesRoundTripThroughFromStorage() {
        PerformanceMode.entries.forEach { mode ->
            assertEquals(mode, PerformanceModePreference.fromStorage(mode.storageValue))
        }
    }
}
