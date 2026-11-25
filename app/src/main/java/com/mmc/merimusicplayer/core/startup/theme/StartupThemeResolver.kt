package com.mmc.merimusicplayer.core.startup.theme

import com.mmc.merimusicplayer.data.model.settings.appearance.ThemeMode
import com.mmc.merimusicplayer.data.model.settings.appearance.ThemePreferenceSnapshot

internal object StartupThemeResolver {
    fun resolveSnapshotUseDark(
        snapshot: ThemePreferenceSnapshot,
        systemDark: Boolean
    ): Boolean {
        return snapshot.resolveUseDark(systemDark)
    }

    fun resolveModeUseDark(
        mode: ThemeMode,
        systemDark: Boolean
    ): Boolean {
        return mode.resolveUseDark(systemDark)
    }
}
