package com.mmc.merimusicplayer.core.startup.theme

import android.content.Context
import com.mmc.merimusicplayer.data.model.settings.appearance.ThemePreferenceSnapshot
import com.mmc.merimusicplayer.data.settings.appearance.readThemePreferenceSnapshotSync

internal object StartupThemeSnapshotProvider {
    fun read(
        context: Context,
        safeModeActive: Boolean
    ): ThemePreferenceSnapshot {
        return if (safeModeActive) {
            ThemePreferenceSnapshot()
        } else {
            readThemePreferenceSnapshotSync(context)
        }
    }
}
