package com.mmc.merimusicplayer.core.di

import com.mmc.merimusicplayer.platform.subsonic.repository.SubsonicRepository

import com.mmc.merimusicplayer.platform.subsonic.api.SubsonicResourceInterceptor

import com.mmc.merimusicplayer.platform.subsonic.api.SubsonicClient

import com.mmc.merimusicplayer.platform.subsonic.auth.SubsonicAccounts

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.mmc.merimusicplayer.core.di/AppContainer
 * Created: 2025/8/19
 */

import com.mmc.merimusicplayer.platform.youtube.api.auth.hasEffectiveAuth
import com.mmc.merimusicplayer.platform.youtube.api.auth.normalized
import kotlinx.coroutines.flow.first
import com.mmc.merimusicplayer.platform.netease.api.auth.NeteaseYdDeviceTokenProvider
import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.mmc.merimusicplayer.platform.bilibili.api.client.BiliClient
import com.mmc.merimusicplayer.platform.bilibili.playback.BiliClientAudioDataSource
import com.mmc.merimusicplayer.platform.bilibili.playback.BiliPlaybackRepository
import com.mmc.merimusicplayer.platform.bilibili.skip.sponsorblock.BiliSponsorBlockRepository
import com.mmc.merimusicplayer.platform.lyrics.api.client.AmllTtmlClient
import com.mmc.merimusicplayer.platform.lyrics.repository.EditableLyricsMatcher
import com.mmc.merimusicplayer.platform.lyrics.repository.AmllLyricsRepository
import com.mmc.merimusicplayer.platform.lyrics.repository.KugouLyricsRepository
import com.mmc.merimusicplayer.platform.lyrics.repository.LrcLibLyricsRepository
import com.mmc.merimusicplayer.platform.lyrics.repository.QQMusicLyricsRepository
import com.mmc.merimusicplayer.platform.lyrics.api.client.KugouLyricsClient
import com.mmc.merimusicplayer.platform.lyrics.api.client.LrcLibClient
import com.mmc.merimusicplayer.platform.netease.api.client.NeteaseClient
import com.mmc.merimusicplayer.BuildConfig
import com.mmc.merimusicplayer.platform.search.api.client.CloudMusicSearchApi
import com.mmc.merimusicplayer.data.model.music.MusicPlatform
import com.mmc.merimusicplayer.platform.search.api.client.QQMusicSearchApi
import com.mmc.merimusicplayer.platform.lyrics.search.SearchManager
import com.mmc.merimusicplayer.platform.youtube.api.client.YouTubeMusicClient
import com.mmc.merimusicplayer.platform.youtube.api.client.YouTubeAccountProfileClient
import com.mmc.merimusicplayer.platform.youtube.repository.YouTubeMusicPlaybackRepository
import com.mmc.merimusicplayer.platform.youtube.api.bootstrap.YouTubePlaybackBootstrapCoordinator
import com.mmc.merimusicplayer.core.download.ManagedDownloadStorage
import com.mmc.merimusicplayer.core.player.download.AudioDownloadManager
import com.mmc.merimusicplayer.core.startup.app.InstrumentationTestRuntime
import com.mmc.merimusicplayer.data.listentogether.ListenTogetherPreferences
import com.mmc.merimusicplayer.data.local.playlist.LocalPlaylistRepository
import com.mmc.merimusicplayer.platform.bilibili.auth.BiliCookieRepository
import com.mmc.merimusicplayer.platform.netease.auth.NeteaseCookieRepository
import com.mmc.merimusicplayer.network.weblogin.ForegroundWebLoginGuard
import com.mmc.merimusicplayer.platform.youtube.auth.YouTubeAuthAutoRefreshManager
import com.mmc.merimusicplayer.platform.youtube.auth.YouTubeAuthRepository
import com.mmc.merimusicplayer.platform.youtube.auth.mergeYouTubeAuthCookieUpdates
import com.mmc.merimusicplayer.data.auth.youtube.YouTubeAuthRotationWorker
import com.mmc.merimusicplayer.data.model.youtube.auth.YOUTUBE_MUSIC_ORIGIN
import com.mmc.merimusicplayer.data.history.PlayHistoryRepository
import com.mmc.merimusicplayer.platform.bilibili.cache.BiliCacheRepositories
import com.mmc.merimusicplayer.data.local.platform.bilibili.BiliVideoSkipRepositoryProvider
import com.mmc.merimusicplayer.platform.netease.NeteasePlaylistCacheRepository
import com.mmc.merimusicplayer.platform.netease.encyclopedia.NeteaseSongEncyclopediaRepository
import com.mmc.merimusicplayer.platform.netease.encyclopedia.SongEncyclopediaRepository
import com.mmc.merimusicplayer.platform.netease.mv.NeteaseMvRepository
import com.mmc.merimusicplayer.platform.youtube.playlist.YouTubeMusicPlaylistCacheRepository
import com.mmc.merimusicplayer.data.playlist.usage.LocalPlaylistPlaybackStatsRepository
import com.mmc.merimusicplayer.data.playlist.usage.PlaylistUsageRepository
import com.mmc.merimusicplayer.data.stats.PlaybackStatsRepository
import com.mmc.merimusicplayer.data.sync.CoverUrlMapper
import com.mmc.merimusicplayer.data.traffic.TrafficStatsRepository
import com.mmc.merimusicplayer.api.ltw.http.ListenTogetherApi
import com.mmc.merimusicplayer.data.ltw.ListenTogetherSessionManager
import com.mmc.merimusicplayer.api.ltw.ws.ListenTogetherWebSocketClient
import com.mmc.merimusicplayer.data.settings.dataStore
import com.mmc.merimusicplayer.data.settings.bootstrap.persistBootstrapSettingsSnapshot
import com.mmc.merimusicplayer.data.settings.playback.persistPlaybackPreferenceSnapshot
import com.mmc.merimusicplayer.data.settings.navigation.synchronizeStartDestinationSnapshot
import com.mmc.merimusicplayer.data.settings.bootstrap.readBootstrapSettingsSnapshotSync
import com.mmc.merimusicplayer.data.settings.SettingsRepository
import com.mmc.merimusicplayer.data.settings.bootstrap.toBootstrapSettingsSnapshot
import com.mmc.merimusicplayer.data.settings.playback.toPlaybackPreferenceSnapshot
import com.mmc.merimusicplayer.platform.youtube.api.transport.buildYouTubeInnertubeRequestHeaders
import com.mmc.merimusicplayer.platform.youtube.api.transport.buildYouTubePageRequestHeaders
import com.mmc.merimusicplayer.platform.youtube.api.transport.buildYouTubeStreamRequestHeaders
import com.mmc.merimusicplayer.platform.youtube.api.transport.isTrustedYouTubeHost
import com.mmc.merimusicplayer.platform.youtube.api.transport.isYouTubeGoogleVideoHost
import com.mmc.merimusicplayer.platform.youtube.api.transport.isYouTubeInnertubeHost
import com.mmc.merimusicplayer.platform.youtube.config.YouTubeFeatureDisabledException
import com.mmc.merimusicplayer.platform.youtube.config.YouTubeFeatureGate
import com.mmc.merimusicplayer.common.logging.NPLogger
import com.mmc.merimusicplayer.platform.comments.CommentMemoryCache
import com.mmc.merimusicplayer.data.model.comments.CommentPlatform
import com.mmc.merimusicplayer.platform.comments.repository.CommentRepository
import com.mmc.merimusicplayer.platform.comments.repository.BiliCommentRepository
import com.mmc.merimusicplayer.platform.comments.repository.NeteaseCommentRepository
import com.mmc.merimusicplayer.network.proxy.DynamicProxySelector
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.LazyThreadSafetyMode

private const val SHARED_HTTP_CONNECT_TIMEOUT_MS = 8_000L
private const val SHARED_HTTP_READ_TIMEOUT_MS = 20_000L
private const val SHARED_HTTP_CALL_TIMEOUT_MS = 0L
private const val SHARED_HTTP_MAX_IDLE_CONNECTIONS = 8
private const val SHARED_HTTP_KEEP_ALIVE_MINUTES = 5L

internal fun configureSharedOkHttpClient(
    builder: OkHttpClient.Builder,
    connectionPool: ConnectionPool = ConnectionPool(
        SHARED_HTTP_MAX_IDLE_CONNECTIONS,
        SHARED_HTTP_KEEP_ALIVE_MINUTES,
        TimeUnit.MINUTES
    )
): OkHttpClient.Builder {
    assert(SHARED_HTTP_CONNECT_TIMEOUT_MS > 0L) { "connect timeout must be positive" }
    assert(SHARED_HTTP_READ_TIMEOUT_MS >= SHARED_HTTP_CONNECT_TIMEOUT_MS) {
        "read timeout must not be shorter than connect timeout"
    }
    assert(SHARED_HTTP_CALL_TIMEOUT_MS == 0L) {
        "shared client call timeout must stay disabled"
    }

    return builder
        .addNetworkInterceptor(com.mmc.merimusicplayer.platform.bilibili.api.stream.BiliStreamCredentialInterceptor())
        .connectTimeout(SHARED_HTTP_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(SHARED_HTTP_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        // 播放, 同步与 WebSocket 共用此客户端, 不能用总时限截断长请求
        .callTimeout(SHARED_HTTP_CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .connectionPool(connectionPool)
        .retryOnConnectionFailure(true)
}

internal fun resolveInitialBypassProxy(
    currentValue: Boolean,
    loadPersistedValue: () -> Boolean
): Boolean = runCatching(loadPersistedValue).getOrDefault(currentValue)

internal data class InitialManagedDownloadSettings(
    val directoryUri: String? = null,
    val directoryLabel: String? = null,
    val fileNameTemplate: String? = null
)

internal fun resolveInitialManagedDownloadSettings(
    currentDirectoryUri: String? = null,
    currentDirectoryLabel: String? = null,
    currentFileNameTemplate: String? = null,
    loadDirectoryUri: () -> String?,
    loadDirectoryLabel: () -> String?,
    loadFileNameTemplate: () -> String?
): InitialManagedDownloadSettings {
    return InitialManagedDownloadSettings(
        directoryUri = runCatching(loadDirectoryUri).getOrDefault(currentDirectoryUri),
        directoryLabel = runCatching(loadDirectoryLabel).getOrDefault(currentDirectoryLabel),
        fileNameTemplate = runCatching(loadFileNameTemplate).getOrDefault(currentFileNameTemplate)
    ).let { resolved ->
        InitialManagedDownloadSettings(
            directoryUri = resolved.directoryUri?.takeIf(String::isNotBlank),
            directoryLabel = resolved.directoryLabel?.takeIf(String::isNotBlank),
            fileNameTemplate = resolved.fileNameTemplate?.takeIf(String::isNotBlank)
        )
    }
}

internal fun handleYouTubeAuthStateChanged(
    bundle: com.mmc.merimusicplayer.data.model.youtube.auth.YouTubeAuthBundle,
    clearBootstrapCache: () -> Unit,
    clearPlaybackAuthBoundCaches: (Boolean) -> Unit,
    evictConnections: () -> Unit,
    youtubeEnabled: Boolean = true,
    warmBootstrapAsync: () -> Unit
) {
    if (!youtubeEnabled) {
        return
    }
    clearBootstrapCache()
    // 只移除旧请求引用, 避免 auth 恢复成功时把当前播放请求自己取消掉
    clearPlaybackAuthBoundCaches(false)
    evictConnections()
    warmYouTubePlaybackIfEnabled(
        youtubeEnabled = youtubeEnabled,
        warmBootstrapAsync = warmBootstrapAsync
    )
}

/**
 * 未登录也要预热
 *
 * bootstrap 和 player.js 这两笔匿名播放照样要付, 而且匿名用户没有任何别的流程会顺带
 * 把它们捂热; 以前卡在登录判断上, 结果最需要预热的那批人反而一次都热不到
 */
internal fun warmYouTubePlaybackIfEnabled(
    youtubeEnabled: Boolean = true,
    backgroundWarmupAllowed: Boolean = !InstrumentationTestRuntime.isActive,
    warmBootstrapAsync: () -> Unit
) {
    if (youtubeEnabled && backgroundWarmupAllowed && !ForegroundWebLoginGuard.isActive) {
        warmBootstrapAsync()
    }
}

private data class YouTubeAuthWarmBootstrapKey(
    val hasEffectiveAuth: Boolean,
    val authorization: String,
    val xGoogAuthUser: String,
    val origin: String,
    val userAgent: String,
)

private fun com.mmc.merimusicplayer.data.model.youtube.auth.YouTubeAuthBundle.toWarmBootstrapKey():
    YouTubeAuthWarmBootstrapKey {
    val normalized = normalized()
    return YouTubeAuthWarmBootstrapKey(
        hasEffectiveAuth = normalized.hasEffectiveAuth(),
        authorization = normalized.authorization,
        xGoogAuthUser = normalized.xGoogAuthUser,
        origin = normalized.origin,
        userAgent = normalized.userAgent
    )
}

/** 将设置发射同步到启动缓存，启动页重读当前真值以排除延迟的旧发射 */
internal suspend fun persistStartupSettingsSnapshots(context: Context, preferences: Preferences) {
    persistBootstrapSettingsSnapshot(context, preferences.toBootstrapSettingsSnapshot())
    persistPlaybackPreferenceSnapshot(context, preferences.toPlaybackPreferenceSnapshot())
    synchronizeStartDestinationSnapshot(context)
}

/**
 * 全局依赖容器, 使用 Service Locator 模式管理 App 的单例
 */
object AppContainer {
    private lateinit var application: Application
    @Volatile
    private var initialized = false
    val applicationContext: Application
        get() = application

    internal fun isInitialized(): Boolean = initialized

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private const val YOUTUBE_DOWNLOAD_PLAYBACK_CALL_TIMEOUT_MS = 20_000L

    val subsonicAccounts by lazy { SubsonicAccounts(application) }
    val subsonicRepository by lazy {
        SubsonicRepository(subsonicAccounts, SubsonicClient(sharedOkHttpClient),
            com.mmc.merimusicplayer.platform.subsonic.repository.SubsonicBrowseCache(
                com.mmc.merimusicplayer.platform.subsonic.repository.SubsonicBrowseRoomStore(
                    com.mmc.merimusicplayer.data.local.database.store.PlatformPlaylistCacheRoomStore(
                        com.mmc.merimusicplayer.data.local.database.MemiUserDataDatabase.getInstance(application)))))
    }

    // 基础 Repo
    val settingsRepo by lazy { SettingsRepository(application) }
    val listenTogetherPreferences by lazy { ListenTogetherPreferences(application) }
    val neteaseCookieRepo by lazy { NeteaseCookieRepository(application) }
    val biliCookieRepo by lazy { BiliCookieRepository(application) }
    val youtubeAuthRepo by lazy { YouTubeAuthRepository(application) }
    internal val youtubeAuthAutoRefreshManager by lazy {
        YouTubeAuthAutoRefreshManager(
            httpClientProvider = { sharedOkHttpClient },
            context = application,
            authStore = youtubeAuthRepo
        )
    }

    internal suspend fun changeYouTubeAuth(updateAuth: () -> Unit) {
        withYouTubeAuthChange(
            updateAuth = updateAuth,
            stopRefresh = youtubeAuthAutoRefreshManager::cancelAndAwaitRefresh,
            clearWebState = {
                com.mmc.merimusicplayer.data.auth.web.clearWebViewLoginState(
                    application,
                    com.mmc.merimusicplayer.data.auth.web.WebLoginPlatform.YOUTUBE
                )
            }
        )
    }

    private val youtubePlaybackBootstrapCoordinator = YouTubePlaybackBootstrapCoordinator()


    val playHistoryRepo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PlayHistoryRepository.getInstance(application)
    }
    val playbackStatsRepo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PlaybackStatsRepository.getInstance(application)
    }
    val trafficStatsRepo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        TrafficStatsRepository.getInstance(application)
    }
    val playlistUsageRepo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PlaylistUsageRepository.getInstance(application)
    }
    val localPlaylistPlaybackStatsRepo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LocalPlaylistPlaybackStatsRepository.getInstance(application)
    }
    val biliFavoriteFolderCacheRepo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        BiliCacheRepositories.createFavoriteFolder(application)
    }
    val biliArchiveCacheRepo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        BiliCacheRepositories.createArchive(application)
    }
    val neteasePlaylistCacheRepo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        NeteasePlaylistCacheRepository(application)
    }
    val youtubeMusicPlaylistCacheRepo by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        YouTubeMusicPlaylistCacheRepository(application)
    }


    // 共享 OkHttpClient: 受 DynamicProxySelector 管理
    val sharedOkHttpClient by lazy {
        val clientBuilder = OkHttpClient.Builder()
            .proxySelector(DynamicProxySelector)
            .addInterceptor(SubsonicResourceInterceptor { subsonicAccounts })
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host.lowercase()
                if (!isYouTubeHost(host)) {
                    return@addInterceptor chain.proceed(request)
                }
                if (!YouTubeFeatureGate.isEnabled()) {
                    throw YouTubeFeatureDisabledException()
                }

                val authSnapshot = youtubeAuthRepo.captureAuthSnapshot()
                val auth = authSnapshot.auth
                val originalHeaders = linkedMapOf<String, String>().apply {
                    request.headers.names().forEach { name ->
                        request.header(name)?.let { value -> put(name, value) }
                    }
                }
                val resolvedHeaders = when {
                    isYouTubeGoogleVideoHost(host) -> auth.buildYouTubeStreamRequestHeaders(
                        original = originalHeaders,
                        refererOrigin = request.header("Referer")
                            .orEmpty()
                            .removeSuffix("/")
                            .ifBlank {
                                request.header("Origin")
                                    .orEmpty()
                                    .removeSuffix("/")
                            }
                            .ifBlank { auth.origin.ifBlank { YOUTUBE_MUSIC_ORIGIN } },
                        streamUrl = request.url.toString()
                    )
                    isYouTubeInnertubeRequest(request) -> auth.buildYouTubeInnertubeRequestHeaders(
                        original = originalHeaders,
                        authorizationOrigin = auth.origin.ifBlank { YOUTUBE_MUSIC_ORIGIN },
                        includeAuthorization = true
                    )
                    else -> auth.buildYouTubePageRequestHeaders(
                        original = originalHeaders
                    )
                }
                val builder = request.newBuilder()
                request.headers.names().forEach { name ->
                    builder.removeHeader(name)
                }
                resolvedHeaders.forEach { (name, value) ->
                    builder.header(name, value)
                }
                val response = chain.proceed(builder.build())
                val setCookieHeaders = response.headers.values("Set-Cookie")
                if (setCookieHeaders.isNotEmpty()) {
                    runCatching {
                        youtubeAuthRepo.updateAuthIfCurrent(authSnapshot) { current ->
                            mergeYouTubeAuthCookieUpdates(
                                base = current,
                                setCookieHeaders = setCookieHeaders
                            )
                        }
                    }.onFailure { error ->
                        NPLogger.w("AppContainer", "Failed to merge YouTube Set-Cookie headers", error)
                    }
                }
                response
            }
        configureSharedOkHttpClient(clientBuilder).build()
    }

    // 网络客户端
    val neteaseClient by lazy {
        NeteaseClient {
            NeteaseYdDeviceTokenProvider(applicationContext)
                .getCommentToken()
        }.also { client ->
            neteaseCookieRepo.withCurrentCookies { cookies ->
                client.setPersistedCookies(cookies)
            }
        }
    }

    val biliClient by lazy { BiliClient(biliCookieRepo, client = sharedOkHttpClient) }

    /** 网易云歌曲 → MV 编号的查询缓存，歌单角标与播放页共用。 */
    val neteaseMvRepository by lazy { NeteaseMvRepository(neteaseClient) }

    /** 歌曲百科仓库；数据源为网易云，其它来源返回 NotSupported。 */
    val songEncyclopediaRepository: SongEncyclopediaRepository by lazy {
        NeteaseSongEncyclopediaRepository(neteaseClient)
    }

    private val commentCache = CommentMemoryCache()
    private val neteaseComments = NeteaseCommentRepository(commentCache) { neteaseClient }
    private val biliComments = BiliCommentRepository(commentCache) { biliClient }

    internal fun commentRepositoryFor(platform: CommentPlatform): CommentRepository = when (platform) {
        CommentPlatform.NETEASE -> neteaseComments
        CommentPlatform.BILIBILI -> biliComments
    }
    internal val biliSponsorBlockRepository by lazy { BiliSponsorBlockRepository(sharedOkHttpClient) }
    internal val biliVideoSkipRepository by lazy { BiliVideoSkipRepositoryProvider.getInstance(application) }
    private val youtubeMusicClientDelegate = lazy {
        YouTubeMusicClient(
            authRepo = youtubeAuthRepo,
            okHttpClient = sharedOkHttpClient,
            authAutoRefreshManager = youtubeAuthAutoRefreshManager
        )
    }
    val youtubeMusicClient: YouTubeMusicClient
        get() = youtubeMusicClientDelegate.value

    // 资料请求保留授权快照，不经过会更新账号 Cookie 的共享拦截器
    private val youtubeAccountProfileHttpClientDelegate = lazy {
        val clientBuilder = OkHttpClient.Builder()
            .proxySelector(DynamicProxySelector)
            .followRedirects(false)
            .addInterceptor { chain ->
                if (!YouTubeFeatureGate.isEnabled()) {
                    throw YouTubeFeatureDisabledException()
                }
                chain.proceed(chain.request())
            }
        configureSharedOkHttpClient(clientBuilder).build()
    }
    val youtubeAccountProfileClient by lazy {
        YouTubeAccountProfileClient(youtubeAccountProfileHttpClientDelegate.value)
    }

    // 功能 Repo 和 API
    val biliPlaybackRepository by lazy {
        val dataSource = BiliClientAudioDataSource(biliClient)
        BiliPlaybackRepository(dataSource) { settingsRepo.biliAudioQualityFlow.first() }
    }
    private val youtubeMusicPlaybackRepositoryDelegate = lazy {
        YouTubeMusicPlaybackRepository(
            okHttpClient = sharedOkHttpClient,
            audioQualityProvider = { settingsRepo.youtubeAudioQualityFlow.first() },
            playbackSourceProvider = { settingsRepo.youtubePlaybackSourceFlow.first() },
            authProvider = youtubeAuthRepo::getAuthOnce,
            authAutoRefreshManager = youtubeAuthAutoRefreshManager,
            applicationContext = application,
            bootstrapCoordinator = youtubePlaybackBootstrapCoordinator
        )
    }
    val youtubeMusicPlaybackRepository: YouTubeMusicPlaybackRepository
        get() = youtubeMusicPlaybackRepositoryDelegate.value
    private val youtubeMusicDownloadPlaybackRepositoryDelegate = lazy {
        YouTubeMusicPlaybackRepository(
            okHttpClient = sharedOkHttpClient.newBuilder()
                .callTimeout(YOUTUBE_DOWNLOAD_PLAYBACK_CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build(),
            audioQualityProvider = { settingsRepo.youtubeAudioQualityFlow.first() },
            playbackSourceProvider = { settingsRepo.youtubePlaybackSourceFlow.first() },
            authProvider = youtubeAuthRepo::getAuthOnce,
            authAutoRefreshManager = youtubeAuthAutoRefreshManager,
            applicationContext = application,
            bootstrapCoordinator = youtubePlaybackBootstrapCoordinator
        )
    }
    val youtubeMusicDownloadPlaybackRepository: YouTubeMusicPlaybackRepository
        get() = youtubeMusicDownloadPlaybackRepositoryDelegate.value

    val cloudMusicSearchApi by lazy {
        CloudMusicSearchApi(neteaseClient, sharedOkHttpClient, debugLogging = BuildConfig.DEBUG)
    }
    val qqMusicSearchApi by lazy {
        QQMusicLyricsRepository(
            api = QQMusicSearchApi(sharedOkHttpClient, debugLogging = BuildConfig.DEBUG),
            amllTtmlClient = amllTtmlClient,
            amllLyricsEnabledProvider = { settingsRepo.amllLyricsEnabledFlow.first() }
        )
    }
    val searchManager by lazy {
        SearchManager { platform ->
            when (platform) {
                MusicPlatform.CLOUD_MUSIC -> cloudMusicSearchApi
                MusicPlatform.QQ_MUSIC -> qqMusicSearchApi
            }
        }
    }
    val lrcLibClient by lazy { LrcLibLyricsRepository(LrcLibClient(sharedOkHttpClient)) }
    val amllTtmlClient by lazy { AmllLyricsRepository(AmllTtmlClient(sharedOkHttpClient)) }
    val kugouLyricsClient by lazy { KugouLyricsRepository(KugouLyricsClient(sharedOkHttpClient)) }
    val editableLyricsMatcher by lazy {
        EditableLyricsMatcher(
            cloudMusicSearchApi = cloudMusicSearchApi,
            qqMusicSearchApi = qqMusicSearchApi,
            kugouLyricsClient = kugouLyricsClient,
            lrcLibClient = lrcLibClient,
            amllTtmlClient = amllTtmlClient,
            youtubeMusicClient = youtubeMusicClient
        )
    }
    val lanListenTogether by lazy { com.mmc.merimusicplayer.core.di.lan.AppLanModule.create() }
    val listenTogetherApi by lazy { ListenTogetherApi(sharedOkHttpClient) }
    private val listenTogetherOkHttpClient by lazy {
        sharedOkHttpClient.newBuilder()
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
    }
    val listenTogetherWebSocketClient by lazy { ListenTogetherWebSocketClient(listenTogetherOkHttpClient) }
    val listenTogetherSessionManager by lazy {
        ListenTogetherSessionManager(
            api = listenTogetherApi,
            webSocketClient = listenTogetherWebSocketClient,
            playback = com.mmc.merimusicplayer.core.player.ltw.PlayerManagerListenTogetherHost,
            platform = com.mmc.merimusicplayer.core.di.ltw.AppListenTogetherPlatform,
            songMapper = com.mmc.merimusicplayer.core.player.ltw.PlayerListenTogetherSongMapper
        )
    }

    fun launchBackgroundIo(block: suspend CoroutineScope.() -> Unit) = scope.launch(block = block)

    fun pauseYouTubeBackgroundWebWorkForForegroundLogin() {
        if (!::application.isInitialized) {
            return
        }
        if (youtubeMusicPlaybackRepositoryDelegate.isInitialized()) {
            youtubeMusicPlaybackRepositoryDelegate.value.clearAuthBoundCaches(
                cancelInFlightPlayableAudio = false
            )
        }
        if (youtubeMusicDownloadPlaybackRepositoryDelegate.isInitialized()) {
            youtubeMusicDownloadPlaybackRepositoryDelegate.value.clearAuthBoundCaches(
                cancelInFlightPlayableAudio = false
            )
        }
    }

    fun initialize(app: Application) {
        this.application = app
        initialized = true
        com.mmc.merimusicplayer.core.integration.download.installDownloadDependencies()
        com.mmc.merimusicplayer.core.di.player.installPlayerDependencies(app)
        com.mmc.merimusicplayer.data.local.media.source.LocalMediaHostAccess.bind(
            downloads = com.mmc.merimusicplayer.core.download.host.media.AndroidLocalMediaDownloads,
            covers = com.mmc.merimusicplayer.core.download.host.media.AndroidLocalMediaCovers,
            crashLogs = com.mmc.merimusicplayer.data.local.media.source.CrashLogCleanup(
                com.mmc.merimusicplayer.core.crash.ExceptionHandler::clearCrashLogs
            )
        )
        com.mmc.merimusicplayer.data.network.DataHttpClients.bind { sharedOkHttpClient }
        com.mmc.merimusicplayer.data.sync.policy.SyncCoverUrlProjection.bind(
            prepare = { subsonicAccounts.load() },
            project = { subsonicAccounts.portableCoverUrl(it) }
        )
        com.mmc.merimusicplayer.data.sync.host.SyncPlaybackActivity.bind {
            com.mmc.merimusicplayer.core.player.PlayerManager.playbackControlPlayingFlow.value
        }
        com.mmc.merimusicplayer.data.auth.youtube.YouTubeRotationHosts.bind(
            object : com.mmc.merimusicplayer.data.auth.youtube.YouTubeRotationHost {
                override fun isInitialized() = AppContainer.isInitialized()
                override suspend fun isYouTubeEnabled() = settingsRepo.youtubeEnabledFlow.first()
                override val authStore get() = youtubeAuthRepo
            }
        )
        com.mmc.merimusicplayer.data.local.database.maintenance.LegacyJsonCleanupRequests.bind(
            com.mmc.merimusicplayer.core.startup.LegacyJsonCleanupScheduler::schedule
        )
        AudioDownloadManager.initialize(app)
        warmLocalPlaylistRepository()
        warmBiliVideoSkipRepository()
        warmCoverUrlMapper()
        primeProxySetting()
        startCookieObserver()
        startYouTubeAuthObserver()
        startSettingsObserver()
        warmYouTubePlaybackOnAppStart()
    }

    private fun warmLocalPlaylistRepository() {
        scope.launch {
            runCatching {
                val repository = LocalPlaylistRepository.getInstance(application)
                if (!repository.awaitInitialized()) {
                    NPLogger.e("AppContainer", "Local playlist preload failed")
                }
            }.onFailure { error ->
                NPLogger.e("AppContainer", "Failed to preload local playlists", error)
            }
        }
    }

    private fun warmBiliVideoSkipRepository() {
        scope.launch {
            runCatching {
                BiliVideoSkipRepositoryProvider.getInstance(application)
            }.onFailure { error ->
                NPLogger.e("AppContainer", "Failed to preload Bili video skip rules", error)
            }
        }
    }

    private fun warmCoverUrlMapper() {
        scope.launch {
            runCatching {
                CoverUrlMapper.getInstance(application)
                subsonicAccounts.load()
            }.onFailure { error ->
                NPLogger.e("AppContainer", "Failed to preload cover URL mappings", error)
            }
        }
    }

    private fun primeProxySetting() {
        val initialBootstrapSettings = readBootstrapSettingsSnapshotSync(application)
        DynamicProxySelector.bypassProxy = initialBootstrapSettings.bypassProxy
        YouTubeFeatureGate.update(initialBootstrapSettings.youtubeEnabled)
        ManagedDownloadStorage.primeSettings(
            directoryUri = initialBootstrapSettings.downloadDirectoryUri,
            directoryLabel = initialBootstrapSettings.downloadDirectoryLabel,
            fileNameTemplate = initialBootstrapSettings.downloadFileNameTemplate
        )
    }

    private fun startCookieObserver() {
        neteaseCookieRepo.cookieFlow
            .onEach { cookies ->
                neteaseCookieRepo.withCurrentCookiesIfMatches(cookies) { currentCookies ->
                    neteaseClient.setPersistedCookies(currentCookies)
                }
            }
            .launchIn(scope)
    }

    private fun startYouTubeAuthObserver() {
        youtubeAuthRepo.authFlow
            .drop(1)
            .distinctUntilChangedBy { bundle -> bundle.toWarmBootstrapKey() }
            .onEach { bundle ->
                if (!YouTubeFeatureGate.isEnabled()) {
                    return@onEach
                }
                handleYouTubeAuthStateChanged(
                    bundle = bundle,
                    clearBootstrapCache = youtubeMusicClient::clearBootstrapCache,
                    clearPlaybackAuthBoundCaches = { cancelInFlight ->
                        youtubeMusicPlaybackRepository.clearAuthBoundCaches(cancelInFlight)
                        youtubeMusicDownloadPlaybackRepository.clearAuthBoundCaches(cancelInFlight)
                    },
                    evictConnections = sharedOkHttpClient.connectionPool::evictAll,
                    youtubeEnabled = YouTubeFeatureGate.isEnabled(),
                    warmBootstrapAsync = youtubeMusicPlaybackRepository::warmBootstrapAsync
                )
            }
            .launchIn(scope)
    }

    /**
     * 将 DataStore 真值同步到启动快照，并更新依赖设置的运行时行为
     * 统一观察也覆盖配置导入等绕过设置页 setter 的写入入口
     */
    private fun startSettingsObserver() {
        application.dataStore.data
            .onEach { preferences ->
                persistStartupSettingsSnapshots(application, preferences)
            }
            .launchIn(scope)

        settingsRepo.bypassProxyFlow
            .onEach { enabled ->
                applyBypassProxyChange(
                    enabled = enabled,
                    currentValue = DynamicProxySelector.bypassProxy,
                    applyValue = { DynamicProxySelector.bypassProxy = it },
                    onChanged = {
                        sharedOkHttpClient.connectionPool.evictAll()
                        if (youtubeAccountProfileHttpClientDelegate.isInitialized()) {
                            youtubeAccountProfileHttpClientDelegate.value.connectionPool.evictAll()
                        }
                        neteaseClient.evictConnections()
                        AudioDownloadManager.notifyRecoveryOpportunity("proxy_changed")
                    }
                )
            }
            .launchIn(scope)

        settingsRepo.downloadDirectoryUriFlow
            .onEach { uri ->
                ManagedDownloadStorage.updateCustomDirectoryUri(uri)
            }
            .launchIn(scope)

        settingsRepo.downloadDirectoryLabelFlow
            .onEach { label ->
                ManagedDownloadStorage.updateCustomDirectoryLabel(label)
            }
            .launchIn(scope)

        settingsRepo.downloadFileNameTemplateFlow
            .onEach { template ->
                ManagedDownloadStorage.updateDownloadFileNameTemplate(template)
            }
            .launchIn(scope)

        settingsRepo.youtubeEnabledFlow
            .onEach { enabled ->
                val wasEnabled = YouTubeFeatureGate.isEnabled()
                YouTubeFeatureGate.update(enabled)
                if (wasEnabled && !enabled) {
                    YouTubeAuthRotationWorker.cancelPeriodicRotation(application)
                    if (youtubeMusicClientDelegate.isInitialized()) {
                        youtubeMusicClientDelegate.value.clearBootstrapCache()
                    }
                    if (youtubeMusicPlaybackRepositoryDelegate.isInitialized()) {
                        youtubeMusicPlaybackRepositoryDelegate.value.clearAuthBoundCaches()
                    }
                    if (youtubeMusicDownloadPlaybackRepositoryDelegate.isInitialized()) {
                        youtubeMusicDownloadPlaybackRepositoryDelegate.value.clearAuthBoundCaches()
                    }
                    AudioDownloadManager.cancelActiveYouTubeDownloads()
                    cancelYouTubeCalls()
                } else if (!wasEnabled && enabled) {
                    YouTubeAuthRotationWorker.schedulePeriodicRotation(application)
                    warmYouTubePlaybackOnAppStart()
                }
            }
            .launchIn(scope)
    }

    internal fun warmYouTubePlaybackOnAppStart(
        backgroundWarmupAllowed: () -> Boolean = { !InstrumentationTestRuntime.isActive },
        loadWarmBootstrap: () -> (() -> Unit) = {
            youtubeMusicPlaybackRepository::warmBootstrapAsync
        }
    ) {
        if (!YouTubeFeatureGate.isEnabled()) {
            return
        }
        warmYouTubePlaybackIfEnabled(
            youtubeEnabled = YouTubeFeatureGate.isEnabled(),
            backgroundWarmupAllowed = backgroundWarmupAllowed(),
            warmBootstrapAsync = { loadWarmBootstrap().invoke() }
        )
    }

    private fun cancelYouTubeCalls() {
        val calls = sharedOkHttpClient.dispatcher.queuedCalls() +
            sharedOkHttpClient.dispatcher.runningCalls()
        calls.filter { call -> isYouTubeHost(call.request().url.host) }
            .forEach { call -> call.cancel() }
        if (youtubeAccountProfileHttpClientDelegate.isInitialized()) {
            youtubeAccountProfileHttpClientDelegate.value.dispatcher.cancelAll()
        }
    }

    private fun isYouTubeHost(host: String): Boolean {
        return isTrustedYouTubeHost(host)
    }

    private fun isYouTubeInnertubeRequest(request: Request): Boolean {
        val host = request.url.host.lowercase()
        val path = request.url.encodedPath.lowercase()
        return isYouTubeInnertubeHost(host) || path.startsWith("/youtubei/")
    }
}
