package com.mmc.merimusicplayer.ui.component

import androidx.compose.ui.Modifier
import com.mmc.merimusicplayer.ui.component.sheet.bottomSheetDragBlocker as newBottomSheetDragBlocker
import com.mmc.merimusicplayer.ui.component.sheet.bottomSheetScrollGuard as newBottomSheetScrollGuard

fun Modifier.bottomSheetScrollGuard(
    allowDownwardToParent: (() -> Boolean)? = null
): Modifier =
    newBottomSheetScrollGuard(allowDownwardToParent)

fun Modifier.bottomSheetDragBlocker(): Modifier =
    newBottomSheetDragBlocker()
