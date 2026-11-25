package com.mmc.merimusicplayer.core.startup.app

import com.mmc.merimusicplayer.data.model.server.ServerSongRef
import com.mmc.merimusicplayer.platform.subsonic.api.SubsonicCoverReference
import com.mmc.merimusicplayer.platform.subsonic.auth.SubsonicProfile
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val markedCoverInCacheKey = Regex("https?://[^\\s|]*?#neri-cover=v1:[0-9a-f]{64}(?=$|[|:])")

internal fun serverCoverCacheAliases(key: String, profiles: List<SubsonicProfile>): List<String> {
    if (profiles.isEmpty()) return emptyList()
    val urls = if (key.startsWith("https://") || key.startsWith("http://")) {
        listOf(key)
    } else {
        if (!key.startsWith("nowplaying-cover|") && !key.startsWith("nowplaying-blur:") &&
            !key.startsWith("playlist-row-cover|") && !key.contains("|data=")) return emptyList()
        markedCoverInCacheKey.findAll(key).map { it.value }.distinct().toList()
    }
    val url = urls.singleOrNull() ?: return emptyList()
    val reference = SubsonicCoverReference.parse(url.toHttpUrlOrNull() ?: return emptyList()) ?: return emptyList()
    return profiles.asSequence().filter { it.enabled && reference.matches(it) }.map { profile ->
        val legacy = ServerSongRef(profile.id, reference.coverArtId).resourceUrl("cover")
        key.replace(url, legacy)
    }.distinct().toList()
}
