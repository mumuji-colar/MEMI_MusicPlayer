package moe.ouom.neriplayer.data.model.settings.appearance

/**
 * Whole-app visual language.
 *
 * [Clean] was the preview style: the settings entry, the startup prompt and every writer
 * are gone, so nothing can select it anymore. The constant is kept only so old backups and
 * cached theme snapshots that still contain `ui_style=clean` keep parsing — [fromStorage]
 * folds that value back to [Material].
 */
enum class UiStyle(val storageValue: String) {
    Material("material"),

    /** 已下线的预览风格，仅用于解析历史存储值，不会再被任何界面选中。 */
    Clean("clean");

    companion object {
        const val DEFAULT_STORAGE_VALUE = "material"

        fun fromStorage(value: String?): UiStyle =
            if (value == Clean.storageValue) {
                Material
            } else {
                entries.firstOrNull { it.storageValue == value } ?: Material
            }
    }
}
