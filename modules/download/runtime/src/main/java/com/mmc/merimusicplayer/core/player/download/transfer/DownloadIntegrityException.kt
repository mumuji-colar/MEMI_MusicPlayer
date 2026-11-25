package com.mmc.merimusicplayer.core.player.download.transfer

import java.io.IOException

internal class DownloadIntegrityException(
    val errorCode: String,
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)
