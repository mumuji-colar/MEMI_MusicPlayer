package com.mmc.merimusicplayer.core.download.host.media

import android.content.Context
import java.io.File
import com.mmc.merimusicplayer.core.download.ManagedDownloadStorage
import com.mmc.merimusicplayer.data.model.download.naming.ParsedManagedDownloadFileName
import com.mmc.merimusicplayer.core.download.naming.candidateManagedDownloadFileNameTemplates
import com.mmc.merimusicplayer.core.download.naming.parseManagedDownloadBaseName
import com.mmc.merimusicplayer.core.download.storage.root.ManagedDownloadRootResolver
import com.mmc.merimusicplayer.core.download.storage.snapshot.CURRENT_SNAPSHOT_CACHE_FILE_NAME
import com.mmc.merimusicplayer.core.player.download.AudioDownloadManager
import com.mmc.merimusicplayer.data.local.media.source.LocalMediaCoverAccess
import com.mmc.merimusicplayer.data.local.media.source.LocalMediaDownloadAccess
import com.mmc.merimusicplayer.data.model.SongItem
import com.mmc.merimusicplayer.data.model.download.DownloadLibraryEntry
import com.mmc.merimusicplayer.data.model.download.DownloadLibrarySnapshot

object AndroidLocalMediaDownloads : LocalMediaDownloadAccess {
    override val snapshotCacheFileName: String
        get() = CURRENT_SNAPSHOT_CACHE_FILE_NAME

    override fun defaultRootDirectory(context: Context): File = ManagedDownloadRootResolver.defaultRootDirectory(context)
    override fun configuredDirectoryUri(): String? = ManagedDownloadStorage.configuredDirectoryUri()
    override fun currentDownloadFileNameTemplate(): String? = ManagedDownloadStorage.currentDownloadFileNameTemplate()
    override fun cachedDownloadLibrarySnapshot(context: Context, restorePersisted: Boolean): DownloadLibrarySnapshot? =
        ManagedDownloadStorage.cachedDownloadLibrarySnapshot(context, restorePersisted)

    override suspend fun buildDownloadLibrarySnapshot(context: Context, forceRefresh: Boolean): DownloadLibrarySnapshot =
        ManagedDownloadStorage.buildDownloadLibrarySnapshot(context, forceRefresh)

    override fun peekDownloadedAudio(song: SongItem): DownloadLibraryEntry? = ManagedDownloadStorage.peekDownloadedAudio(song)
    override fun peekCoverReference(audio: DownloadLibraryEntry): String? {
        require(audio is ManagedDownloadStorage.StoredEntry)
        return ManagedDownloadStorage.peekCoverReference(audio)
    }

    override fun toPlayableUri(reference: String?): String? = ManagedDownloadStorage.toPlayableUri(reference)
    override fun isKnownManagedDownloadDocumentId(documentId: String, treeDocumentId: String?): Boolean =
        ManagedDownloadStorage.isKnownManagedDownloadDocumentId(documentId, treeDocumentId)

    override fun isManagedDownloadRelativePath(relativePath: String?, treeDocumentId: String?): Boolean =
        ManagedDownloadStorage.isManagedDownloadRelativePath(relativePath, treeDocumentId)

    override fun resolveManagedAudioDisplayName(context: Context, song: SongItem): String? =
        ManagedDownloadStorage.resolveManagedAudioDisplayName(context, song)

    override fun isLikelyManagedDownloadSong(context: Context, song: SongItem): Boolean =
        ManagedDownloadStorage.isLikelyManagedDownloadSong(context, song)

    override fun candidateFileNameTemplates(activeTemplate: String?): List<String> =
        candidateManagedDownloadFileNameTemplates(activeTemplate)

    override fun parseBaseName(baseName: String, template: String?): ParsedManagedDownloadFileName? =
        parseManagedDownloadBaseName(baseName, template)
}

object AndroidLocalMediaCovers : LocalMediaCoverAccess {
    override fun peekLocalCoverUri(song: SongItem): String? = AudioDownloadManager.peekLocalCoverUri(song)
    override fun getLocalCoverUri(context: Context, song: SongItem, resolveLocalMediaFallback: Boolean): String? =
        AudioDownloadManager.getLocalCoverUri(context, song, resolveLocalMediaFallback)
}
