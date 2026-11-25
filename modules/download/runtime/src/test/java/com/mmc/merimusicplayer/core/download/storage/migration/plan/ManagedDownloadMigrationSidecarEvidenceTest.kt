package com.mmc.merimusicplayer.core.download.storage.migration.plan

import com.mmc.merimusicplayer.core.download.ManagedDownloadStorage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedDownloadMigrationSidecarEvidenceTest {
    @Test
    fun `metadata less audio needs sidecar evidence unless the root allows it`() {
        val rootEntries = listOf(directory("Covers"), file("notes.txt"), file("a.flac"), file("b.MP3"))

        assertTrue(requiresSidecarEvidence(rootEntries, allowMetadataLessAudio = false))
        assertFalse(requiresSidecarEvidence(rootEntries, allowMetadataLessAudio = true))
    }

    @Test
    fun `a metadata sidecar or a root without audio needs no extra evidence`() {
        assertFalse(
            requiresSidecarEvidence(listOf(file("a.flac"), file("a.flac.npmeta.json")), allowMetadataLessAudio = false)
        )
        assertFalse(
            requiresSidecarEvidence(
                listOf(directory("Lyrics"), file("cover.jpg"), file("notes.txt")),
                allowMetadataLessAudio = false
            )
        )
    }

    private fun requiresSidecarEvidence(
        rootEntries: List<ManagedDownloadStorage.StoredEntry>,
        allowMetadataLessAudio: Boolean
    ): Boolean = ManagedDownloadMigrationEntryCollector.requiresSidecarEvidence(rootEntries, allowMetadataLessAudio)

    private fun file(name: String) = entry(name, isDirectory = false)

    private fun directory(name: String) = entry(name, isDirectory = true)

    private fun entry(name: String, isDirectory: Boolean) = ManagedDownloadStorage.StoredEntry(
        name = name,
        reference = "/music/MemiPlayer/$name",
        mediaUri = "file:///music/MemiPlayer/$name",
        localFilePath = "/music/MemiPlayer/$name",
        sizeBytes = if (isDirectory) 0L else 32L,
        lastModifiedMs = 1L,
        isDirectory = isDirectory
    )
}
