package com.mmc.merimusicplayer.core.player.url

import com.mmc.merimusicplayer.data.identity.recoverNeteaseRemoteSourceFromStaleLocalCopy
import com.mmc.merimusicplayer.data.local.media.LocalSongSupport
import com.mmc.merimusicplayer.data.local.media.source.ResolvedLocalMediaSource
import com.mmc.merimusicplayer.data.local.playlist.LocalPlaylistRepository
import com.mmc.merimusicplayer.data.model.SongItem

internal fun recoverNeteaseSourceAfterMissingLocal(
    song: SongItem,
    rawLocalReference: String?,
    sourceContext: ResolvedLocalMediaSource? = null,
    explicitLocalFileReference: (String) -> Boolean? = LocalPlaylistRepository::peekExplicitLocalFileReference
): SongItem? {
    val reference = rawLocalReference ?: return null
    val snapshotReference = LocalSongSupport.validExplicitLocalSourceReference(song)
    fun explicitEvidence(candidate: String): Boolean? =
        if (snapshotReference == candidate) true else explicitLocalFileReference(candidate)

    val unverifiedDeclaredReference = sourceContext?.unverifiedDeclaredReferenceFor(song, reference)
    if (unverifiedDeclaredReference != null && explicitEvidence(unverifiedDeclaredReference) != false) return null
    val provenanceReference = sourceContext?.anchorFor(song, reference) ?: reference
    if (explicitEvidence(provenanceReference) != false) return null
    return song.recoverNeteaseRemoteSourceFromStaleLocalCopy()
}
