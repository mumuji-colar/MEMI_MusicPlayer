package com.mmc.merimusicplayer.data.sync.mapping

import com.mmc.merimusicplayer.data.sync.CoverUrlMapper

internal fun sanitizeCoverUrlForSync(
    coverUrl: String?,
    mapper: CoverUrlMapper? = null
): String? {
    val normalizedUrl = coverUrl?.trim().orEmpty()
    val directUrl = com.mmc.merimusicplayer.data.sync.policy.sanitizeCoverUrlForSync(normalizedUrl)
    if (directUrl != null) return directUrl
    return mapper?.getSyncableNetworkUrl(normalizedUrl)
}
