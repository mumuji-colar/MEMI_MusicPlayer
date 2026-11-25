package com.mmc.merimusicplayer.core.download

import com.mmc.merimusicplayer.core.download.catalog.shouldRestoreManagedLibraryItem
import org.junit.Assert.assertFalse
import org.junit.Test

class ManagedLibraryItemRestorePolicyTest {

    @Test
    fun `unknown and in-progress library states never restore into the catalog`() {
        assertFalse(shouldRestoreManagedLibraryItem(""))
        assertFalse(shouldRestoreManagedLibraryItem("UNKNOWN"))
        assertFalse(shouldRestoreManagedLibraryItem("QUEUED"))
        assertFalse(shouldRestoreManagedLibraryItem("DOWNLOADING"))
    }
}
