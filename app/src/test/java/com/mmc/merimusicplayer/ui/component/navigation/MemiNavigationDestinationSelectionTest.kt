package com.mmc.merimusicplayer.ui.component.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import com.mmc.merimusicplayer.navigation.Destinations
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class MemiNavigationDestinationSelectionTest {

    @Test
    fun `no current destination selects no tab`() {
        assertFalse(isMemiNavigationDestinationSelected(null, Destinations.Home))
    }

    @Test
    fun `a top level route selects only its own tab`() {
        val explore = destination(route = "explore")

        assertTrue(isMemiNavigationDestinationSelected(explore, Destinations.Explore))
        assertFalse(isMemiNavigationDestinationSelected(explore, Destinations.Home))
    }

    @Test
    fun `a nested destination selects the tab of an enclosing graph`() {
        val root = graph(route = null, parent = null)
        val library = graph(route = "library", parent = root)
        val playlistDetail = destination(route = "playlist_detail/{id}", parent = library)

        assertTrue(isMemiNavigationDestinationSelected(playlistDetail, Destinations.Library))
        assertFalse(isMemiNavigationDestinationSelected(playlistDetail, Destinations.Settings))
    }

    private fun destination(route: String?, parent: NavGraph? = null): NavDestination =
        mock(NavDestination::class.java).also { destination ->
            `when`(destination.route).thenReturn(route)
            `when`(destination.parent).thenReturn(parent)
        }

    private fun graph(route: String?, parent: NavGraph?): NavGraph =
        mock(NavGraph::class.java).also { graph ->
            `when`(graph.route).thenReturn(route)
            `when`(graph.parent).thenReturn(parent)
        }
}
