package com.mmc.merimusicplayer.data.model.sync

data class SyncUploadResolution<TMerged, TVersion>(
    val merged: TMerged,
    val remoteVersion: TVersion,
    val uploadPerformed: Boolean,
    val remoteChangedDuringSync: Boolean
)
