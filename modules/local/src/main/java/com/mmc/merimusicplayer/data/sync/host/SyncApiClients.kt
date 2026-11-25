package com.mmc.merimusicplayer.data.sync.host

import android.content.Context
import com.mmc.merimusicplayer.api.sync.github.GitHubApiClient
import com.mmc.merimusicplayer.api.sync.webdav.WebDavApiClient
import com.mmc.merimusicplayer.common.R as CoreCommonR
import com.mmc.merimusicplayer.data.network.DataHttpClients
import java.io.File

fun createGitHubSyncClient(context: Context, token: String): GitHubApiClient = GitHubApiClient(
    token = token,
    client = DataHttpClients.shared,
    tokenExpiredMessage = context.getString(CoreCommonR.string.github_token_expired_message),
    checkpointDirectory = File(context.filesDir, "sync-v3-github-staging")
)

fun createWebDavSyncClient(context: Context, username: String, password: String): WebDavApiClient = WebDavApiClient(
    username = username,
    password = password,
    client = DataHttpClients.shared,
    authFailureMessage = context.getString(CoreCommonR.string.webdav_auth_failed),
    directoryMissingMessage = context.getString(CoreCommonR.string.webdav_directory_missing),
    accessDeniedMessage = context.getString(CoreCommonR.string.webdav_access_denied)
)
