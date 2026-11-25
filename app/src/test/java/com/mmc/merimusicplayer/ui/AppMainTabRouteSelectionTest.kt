package com.mmc.merimusicplayer.ui

import com.mmc.merimusicplayer.navigation.Destinations
import com.mmc.merimusicplayer.ui.navigation.selectMainTabRouteContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppMainTabRouteSelectionTest {
    @Test
    fun selectsOnlyTheMatchingMainTabContent() {
        listOf(
            Destinations.Home.route to "home",
            Destinations.Explore.route to "explore",
            Destinations.Library.route to "library",
            Destinations.Settings.route to "settings",
            Destinations.Debug.route to "debug"
        ).forEach { (route, expected) ->
            assertEquals(expected, select(route))
        }
        assertNull(select("unsupported"))
    }

    private fun select(route: String) = selectMainTabRouteContent(
        route = route,
        home = "home",
        explore = "explore",
        library = "library",
        settings = "settings",
        debug = "debug"
    )
}
