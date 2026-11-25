package com.mmc.merimusicplayer.testing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmc.merimusicplayer.testutil.grantRuntimePermissions
import com.mmc.merimusicplayer.testutil.playbackRuntimePermissions
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionBootstrapTest {

    @Test
    fun grantPlaybackRuntimePermissions() {
        grantRuntimePermissions(*playbackRuntimePermissions())
    }
}
