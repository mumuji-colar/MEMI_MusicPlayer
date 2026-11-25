package com.mmc.merimusicplayer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import com.mmc.merimusicplayer.navigation.Destinations
import com.mmc.merimusicplayer.ui.component.navigation.DEFAULT_BOTTOM_BAR_SELECTION_ALPHA

@Composable
fun MemiBottomBar(
    items: List<Pair<Destinations, ImageVector>>,
    currentDestination: NavDestination?,
    onItemSelected: (Destinations) -> Unit,
    modifier: Modifier = Modifier,
    selectAlpha: Float = DEFAULT_BOTTOM_BAR_SELECTION_ALPHA
) {
    com.mmc.merimusicplayer.ui.component.navigation.MemiBottomBar(
        items = items,
        currentDestination = currentDestination,
        onItemSelected = onItemSelected,
        modifier = modifier,
        selectAlpha = selectAlpha
    )
}
