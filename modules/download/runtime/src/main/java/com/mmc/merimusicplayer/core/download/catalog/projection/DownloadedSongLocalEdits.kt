package com.mmc.merimusicplayer.core.download.catalog.projection

import com.mmc.merimusicplayer.core.download.catalog.isResolvableLocalReference
import com.mmc.merimusicplayer.data.model.download.DownloadedSong
import com.mmc.merimusicplayer.core.download.policy.localFileNameFromFileReference
import com.mmc.merimusicplayer.core.download.policy.resolvedLocalFileName
import com.mmc.merimusicplayer.data.model.SongItem

internal class DownloadedSongLocalEdits(
    private val existing: DownloadedSong,
    private val updated: SongItem
) {
    private val updatedReference = localReference(updated.mediaUri)
    val customCoverUrl: String? = nonblank(updated.customCoverUrl?.trim())

    val album: String
        get() = updated.album.trim().ifBlank { existing.album }

    val coverPath: String?
        get() = if (customCoverUrl == null) localReference(updated.coverUrl) else existing.coverPath

    val coverUrl: String?
        get() = updated.coverUrl ?: existing.coverUrl

    val mediaUri: String?
        get() = updatedReference ?: existing.mediaUri

    val durationMs: Long
        get() = if (updated.durationMs > 0L) updated.durationMs else existing.durationMs

    val fileName: String?
        get() = nonblank(updated.localFileName)
            ?: localFileNameFromFileReference(updatedReference)
            ?: retainedFileName()

    private fun retainedFileName(): String? {
        val existingReference = nonblank(existing.mediaUri) ?: existing.filePath
        if (updatedReference != null && updatedReference != existingReference) return null
        return existing.resolvedLocalFileName()
    }
}

private fun localReference(value: String?): String? {
    if (value == null) return null
    return value.takeIf(::isResolvableLocalReference)
}

private fun nonblank(value: String?): String? {
    if (value == null) return null
    return value.ifBlank { null }
}
