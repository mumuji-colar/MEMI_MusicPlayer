package moe.ouom.neriplayer.ui.effect.glass

import moe.ouom.neriplayer.data.settings.appearance.EnhancedAdvancedBlurPreference

internal data class AdvancedGlassTokens(
    val blurRadiusDp: Float,
    val tintAlpha: Float,
    val edgeAlpha: Float,
    val samplesBackdrop: Boolean = true
)

internal fun advancedGlassTokens(
    role: AdvancedGlassRole,
    isDarkTheme: Boolean,
    enhancedBlurRadiusDp: Float? = null,
    liquid: Boolean = false
): AdvancedGlassTokens {
    val adjustableRadiusDp = enhancedBlurRadiusDp?.let(EnhancedAdvancedBlurPreference::normalize)
    return when (role) {
        AdvancedGlassRole.MiniPlayer -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 24f,
            tintAlpha = when {
                liquid -> if (isDarkTheme) 0.22f else 0.30f
                isDarkTheme -> 0.30f
                else -> 0.36f
            },
            edgeAlpha = if (liquid) 0.06f else 0.10f
        )
        AdvancedGlassRole.BottomNavigation -> AdvancedGlassTokens(
            // 与迷你播放器同一档：这两条栏现在共用一套玻璃渲染，
            // 底色只比播放条实一点，保证 12sp 的小标签压在花哨封面上还看得清
            blurRadiusDp = adjustableRadiusDp ?: 24f,
            tintAlpha = if (liquid) {
                if (isDarkTheme) 0.40f else 0.46f
            } else {
                if (isDarkTheme) 0.46f else 0.52f
            },
            edgeAlpha = if (liquid) 0.07f else 0.10f
        )
        AdvancedGlassRole.NavigationRail,
        AdvancedGlassRole.ScreenTopTab -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 22f,
            tintAlpha = if (isDarkTheme) 0.16f else 0.18f,
            edgeAlpha = 0.10f
        )
        AdvancedGlassRole.SettingsGroup -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 26f,
            tintAlpha = if (isDarkTheme) 0.30f else 0.34f,
            edgeAlpha = 0.12f
        )
        AdvancedGlassRole.SettingsHeader -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 24f,
            tintAlpha = if (isDarkTheme) 0.28f else 0.32f,
            edgeAlpha = 0.12f
        )
        AdvancedGlassRole.SettingsSection -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 24f,
            tintAlpha = if (isDarkTheme) 0.28f else 0.32f,
            edgeAlpha = 0.10f
        )
        AdvancedGlassRole.PlaylistSheet -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 24f,
            tintAlpha = if (isDarkTheme) 0.24f else 0.18f,
            edgeAlpha = 0.08f
        )
        AdvancedGlassRole.SemanticCard -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 22f,
            tintAlpha = if (isDarkTheme) 0.42f else 0.46f,
            edgeAlpha = 0.10f
        )
        AdvancedGlassRole.ExploreTag -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 18f,
            tintAlpha = if (isDarkTheme) 0.34f else 0.38f,
            edgeAlpha = 0.08f
        )
        AdvancedGlassRole.ThemeModeToggle -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 18f,
            tintAlpha = if (isDarkTheme) 0.36f else 0.40f,
            edgeAlpha = 0.08f
        )
        AdvancedGlassRole.InlineControl -> AdvancedGlassTokens(
            blurRadiusDp = 0f,
            tintAlpha = 1f,
            edgeAlpha = 0f,
            samplesBackdrop = false
        )
    }
}
