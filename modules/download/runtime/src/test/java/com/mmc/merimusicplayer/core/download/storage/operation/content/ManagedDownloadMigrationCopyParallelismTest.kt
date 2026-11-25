package com.mmc.merimusicplayer.core.download.storage.operation.content

import androidx.documentfile.provider.DocumentFile
import java.io.File
import com.mmc.merimusicplayer.core.download.ManagedDownloadStorage
import com.mmc.merimusicplayer.core.download.storage.MIGRATION_COPY_PARALLELISM
import com.mmc.merimusicplayer.core.download.storage.MIGRATION_TREE_COPY_PARALLELISM
import com.mmc.merimusicplayer.core.download.storage.root.ManagedDownloadRootHandle
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class ManagedDownloadMigrationCopyParallelismTest {
    @Test
    fun `file to file copies keep file parallelism while any tree root lowers it`() {
        val fileRoot = ManagedDownloadRootHandle.FileRoot(File("/storage/emulated/0/Music/MemiPlayer"))
        val treeRoot = ManagedDownloadRootHandle.TreeRoot(mock(DocumentFile::class.java))

        assertEquals(
            MIGRATION_COPY_PARALLELISM,
            ManagedDownloadStorage.migrationCopyParallelism(fileRoot, fileRoot)
        )
        assertEquals(
            MIGRATION_TREE_COPY_PARALLELISM,
            ManagedDownloadStorage.migrationCopyParallelism(treeRoot, fileRoot)
        )
        assertEquals(
            MIGRATION_TREE_COPY_PARALLELISM,
            ManagedDownloadStorage.migrationCopyParallelism(fileRoot, treeRoot)
        )
    }
}
