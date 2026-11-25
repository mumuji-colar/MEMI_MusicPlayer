package com.mmc.merimusicplayer.navigation

import com.mmc.merimusicplayer.data.model.navigation.LauncherShortcutAction
import com.mmc.merimusicplayer.data.model.navigation.LauncherShortcutRequest

const val ACTION_LAUNCHER_SHORTCUT_CONTINUE_PLAYBACK =
    "com.mmc.merimusicplayer.action.CONTINUE_PLAYBACK"
const val ACTION_LAUNCHER_SHORTCUT_EXPLORE =
    "com.mmc.merimusicplayer.action.OPEN_EXPLORE"
const val ACTION_LAUNCHER_SHORTCUT_LIBRARY =
    "com.mmc.merimusicplayer.action.OPEN_LIBRARY"
const val ACTION_LAUNCHER_SHORTCUT_SHUFFLE_FAVORITES =
    "com.mmc.merimusicplayer.action.SHUFFLE_FAVORITES"

fun launcherShortcutActionFromIntentAction(action: String?): LauncherShortcutAction? {
    return when (action) {
        ACTION_LAUNCHER_SHORTCUT_CONTINUE_PLAYBACK ->
            LauncherShortcutAction.ContinuePlayback
        ACTION_LAUNCHER_SHORTCUT_EXPLORE ->
            LauncherShortcutAction.OpenExplore
        ACTION_LAUNCHER_SHORTCUT_LIBRARY ->
            LauncherShortcutAction.OpenLibrary
        ACTION_LAUNCHER_SHORTCUT_SHUFFLE_FAVORITES ->
            LauncherShortcutAction.ShuffleFavorites
        else -> null
    }
}

fun launcherShortcutMainTabRoute(action: LauncherShortcutAction): String? {
    return when (action) {
        LauncherShortcutAction.OpenExplore -> Destinations.Explore.route
        LauncherShortcutAction.OpenLibrary -> Destinations.Library.route
        LauncherShortcutAction.ContinuePlayback,
        LauncherShortcutAction.ShuffleFavorites -> null
    }
}
