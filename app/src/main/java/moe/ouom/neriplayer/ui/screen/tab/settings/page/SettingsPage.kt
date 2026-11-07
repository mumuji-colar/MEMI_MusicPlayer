package moe.ouom.neriplayer.ui.screen.tab.settings.page

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import moe.ouom.neriplayer.common.R as CoreCommonR
import moe.ouom.neriplayer.data.settings.AutoSettingsSchema
import moe.ouom.neriplayer.ksp.annotations.AutoSettingsSectionEntry
import moe.ouom.neriplayer.R as AppR

internal enum class SettingsPage(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
) {
    General(
        AutoSettingsSchema.general.metadata,
        Icons.Outlined.Settings
    ),
    Language(
        titleRes = CoreCommonR.string.settings_page_language,
        descriptionRes = CoreCommonR.string.settings_page_language_desc,
        icon = Icons.Outlined.Translate
    ),
    AudioEffects(
        titleRes = AppR.string.settings_audio_effects_title,
        descriptionRes = AppR.string.settings_audio_effects_desc,
        icon = Icons.Outlined.GraphicEq
    ),
    Theme(
        AutoSettingsSchema.theme.metadata,
        Icons.Outlined.Palette
    ),
    Accounts(
        titleRes = CoreCommonR.string.settings_login_platforms,
        descriptionRes = CoreCommonR.string.settings_accounts_desc,
        icon = Icons.Filled.AccountCircle
    ),
    Personalization(
        titleRes = CoreCommonR.string.settings_page_home_start,
        descriptionRes = CoreCommonR.string.settings_page_home_start_desc,
        icon = Icons.Outlined.Tune
    ),
    NowPlaying(
        titleRes = CoreCommonR.string.settings_page_now_playing,
        descriptionRes = CoreCommonR.string.settings_page_now_playing_desc,
        icon = Icons.Outlined.Album
    ),
    Background(
        titleRes = CoreCommonR.string.settings_page_background,
        descriptionRes = CoreCommonR.string.settings_page_background_desc,
        icon = Icons.Outlined.Wallpaper
    ),
    Motion(
        AutoSettingsSchema.motion.metadata,
        Icons.Outlined.Bolt
    ),
    Lyrics(
        titleRes = CoreCommonR.string.settings_page_lyrics,
        descriptionRes = CoreCommonR.string.settings_page_lyrics_desc,
        icon = Icons.Outlined.Lyrics
    ),
    ExternalLyrics(
        titleRes = CoreCommonR.string.settings_page_external_lyrics,
        descriptionRes = CoreCommonR.string.settings_page_external_lyrics_desc,
        icon = Icons.Outlined.Subtitles
    ),
    XiaomiSuperIsland(
        titleRes = AutoSettingsSchema.lyrics.xiaomiSuperIslandLyricEnabled.titleRes,
        descriptionRes = AutoSettingsSchema.lyrics.xiaomiSuperIslandLyricEnabled.descriptionRes,
        icon = Icons.Outlined.Subtitles
    ),
    Network(
        AutoSettingsSchema.network.metadata,
        Icons.Outlined.Router
    ),
    Playback(
        AutoSettingsSchema.playback.metadata,
        Icons.AutoMirrored.Outlined.PlaylistPlay
    ),
    UsbExclusive(
        titleRes = CoreCommonR.string.settings_usb_exclusive_playback,
        descriptionRes = CoreCommonR.string.settings_usb_exclusive_page_desc,
        icon = Icons.Outlined.Usb
    ),
    PlaybackSource(
        titleRes = CoreCommonR.string.settings_playback_source,
        descriptionRes = CoreCommonR.string.settings_playback_source_desc,
        icon = Icons.Outlined.LibraryMusic
    ),
    MusicServers(
        titleRes = CoreCommonR.string.server_manage,
        descriptionRes = CoreCommonR.string.server_settings_desc,
        icon = Icons.Outlined.LibraryMusic
    ),
    AudioQuality(
        AutoSettingsSchema.audioQuality.metadata,
        Icons.Filled.Audiotrack
    ),
    Storage(
        AutoSettingsSchema.storage.metadata,
        Icons.Outlined.Storage
    ),
    StorageCacheDetails(
        titleRes = CoreCommonR.string.settings_storage_cache_details,
        descriptionRes = CoreCommonR.string.settings_storage_cache_details_desc,
        icon = Icons.Outlined.Storage
    ),
    TrafficManagement(
        AutoSettingsSchema.trafficManagement.metadata,
        Icons.Outlined.Analytics
    ),
    Downloads(
        AutoSettingsSchema.download.metadata,
        Icons.Outlined.Download
    ),
    Backup(
        AutoSettingsSchema.backup.metadata,
        Icons.Outlined.Sync
    ),
    ListenTogether(
        titleRes = CoreCommonR.string.listen_together_title,
        descriptionRes = CoreCommonR.string.settings_listen_together_expand,
        icon = Icons.Outlined.Cloud
    ),

    // ---- 1.12.1 重构：一级分类页 ----
    // 下列 10 个是一级分类，由下方原子页按顺序聚合渲染；原子页不再出现在设置主页。
    AccountSources(
        titleRes = CoreCommonR.string.settings_hub_account_sources,
        descriptionRes = CoreCommonR.string.settings_hub_account_sources_desc,
        icon = Icons.Filled.AccountCircle
    ),
    PlaybackHub(
        titleRes = CoreCommonR.string.settings_hub_playback,
        descriptionRes = CoreCommonR.string.settings_hub_playback_desc,
        icon = Icons.AutoMirrored.Outlined.PlaylistPlay
    ),
    Appearance(
        titleRes = CoreCommonR.string.settings_hub_appearance,
        descriptionRes = CoreCommonR.string.settings_hub_appearance_desc,
        icon = Icons.Outlined.Palette
    ),
    MotionPerformance(
        titleRes = CoreCommonR.string.settings_hub_motion,
        descriptionRes = CoreCommonR.string.settings_hub_motion_desc,
        icon = Icons.Outlined.Bolt
    ),
    LayoutHub(
        titleRes = CoreCommonR.string.settings_hub_layout,
        descriptionRes = CoreCommonR.string.settings_hub_layout_desc,
        icon = Icons.Outlined.Album
    ),
    LyricsHub(
        titleRes = CoreCommonR.string.settings_hub_lyrics,
        descriptionRes = CoreCommonR.string.settings_hub_lyrics_desc,
        icon = Icons.Outlined.Lyrics
    ),
    StorageDownloads(
        titleRes = CoreCommonR.string.settings_hub_storage,
        descriptionRes = CoreCommonR.string.settings_hub_storage_desc,
        icon = Icons.Outlined.Storage
    ),
    NetworkTraffic(
        titleRes = CoreCommonR.string.settings_hub_network,
        descriptionRes = CoreCommonR.string.settings_hub_network_desc,
        icon = Icons.Outlined.Router
    ),
    BackupSync(
        titleRes = CoreCommonR.string.settings_hub_backup,
        descriptionRes = CoreCommonR.string.settings_hub_backup_desc,
        icon = Icons.Outlined.Sync
    ),
    GeneralHub(
        titleRes = CoreCommonR.string.settings_hub_general,
        descriptionRes = CoreCommonR.string.settings_hub_general_desc,
        icon = Icons.Outlined.Settings
    ),

    About(
        titleRes = CoreCommonR.string.settings_about,
        descriptionRes = CoreCommonR.string.settings_about_desc,
        icon = Icons.Outlined.Info
    );

    constructor(section: AutoSettingsSectionEntry, fallbackIcon: ImageVector) : this(
        titleRes = section.titleRes,
        descriptionRes = section.descriptionRes,
        icon = section.icon.toSettingsPageIcon(fallbackIcon)
    )
}

/** Mirrors the desktop settings layout: one group per concern, nothing hidden behind another page. */
/** Lets nested settings rows open a second-level page without threading callbacks through. */
internal val LocalSettingsPageNavigator = staticCompositionLocalOf<((SettingsPage) -> Unit)?> { null }

/**
 * 1.12.1 重构后的一级入口顺序（4 张分组卡 / 11 个入口）。
 * 其余枚举值保留为「原子页」，只作为聚合渲染与搜索跳转的内部目标，不再出现在主页。
 */
internal val SettingsHomePageGroups: List<List<SettingsPage>> = listOf(
    listOf(
        SettingsPage.AccountSources,
        SettingsPage.PlaybackHub,
        SettingsPage.LyricsHub
    ),
    listOf(
        SettingsPage.Appearance,
        SettingsPage.MotionPerformance,
        SettingsPage.LayoutHub
    ),
    listOf(
        SettingsPage.StorageDownloads,
        SettingsPage.NetworkTraffic,
        SettingsPage.BackupSync
    ),
    listOf(
        SettingsPage.GeneralHub,
        SettingsPage.About
    )
)

/** 一级入口集合；搜索的「页面」结果只从这里生成。 */
internal val SettingsTopLevelPages: Set<SettingsPage> =
    SettingsHomePageGroups.flatten().toSet()

/** 每个一级分类页由哪些原子页按顺序拼接而成（顺序即卡片顺序）。 */
internal val SettingsHubComposition: Map<SettingsPage, List<SettingsPage>> = mapOf(
    SettingsPage.AccountSources to listOf(
        SettingsPage.Accounts,
        SettingsPage.PlaybackSource
    ),
    SettingsPage.PlaybackHub to listOf(
        SettingsPage.Playback,
        SettingsPage.AudioQuality
    ),
    SettingsPage.Appearance to listOf(
        SettingsPage.Theme,
        SettingsPage.Background
    ),
    SettingsPage.MotionPerformance to listOf(
        SettingsPage.Motion
    ),
    SettingsPage.LayoutHub to listOf(
        SettingsPage.NowPlaying,
        SettingsPage.Personalization
    ),
    SettingsPage.LyricsHub to listOf(
        SettingsPage.Lyrics,
        SettingsPage.ExternalLyrics
    ),
    SettingsPage.StorageDownloads to listOf(
        SettingsPage.Storage,
        SettingsPage.Downloads
    ),
    SettingsPage.NetworkTraffic to listOf(
        SettingsPage.Network,
        SettingsPage.TrafficManagement
    ),
    SettingsPage.BackupSync to listOf(
        SettingsPage.Backup,
        SettingsPage.ListenTogether
    ),
    SettingsPage.GeneralHub to listOf(
        SettingsPage.General,
        SettingsPage.Language
    )
)

/**
 * 各页卡片切片顺序：渲染循环与搜索锚点的卡片数都读这一份清单。
 *
 * 1.20 之前「播放」用 `for (cardIndex in 0..3)`、歌词用集中清单，两套写法并存；
 * 现在统一到这里，少一处改数字时漏改的地方。
 */
internal val PlaybackPageCards = listOf(0, 1, 2)
internal val MotionPageCards = listOf(0, 1, 2, 3)
internal val LyricsPageCards = listOf(4, 1, 3)
internal val ExternalLyricsPageCards = listOf(0, 2)

internal fun SettingsPage.backTargetPage(): SettingsPage? = when (this) {
    // 三级页回到各自的一级分类
    SettingsPage.MusicServers -> SettingsPage.AccountSources
    SettingsPage.StorageCacheDetails -> SettingsPage.StorageDownloads
    SettingsPage.XiaomiSuperIsland -> SettingsPage.LyricsHub
    SettingsPage.AudioEffects -> SettingsPage.PlaybackHub
    SettingsPage.UsbExclusive -> SettingsPage.PlaybackHub
    else -> null
}
