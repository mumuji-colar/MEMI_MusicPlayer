package com.mmc.merimusicplayer.platform.comments.repository

import com.mmc.merimusicplayer.platform.comments.CommentApiException
import com.mmc.merimusicplayer.data.model.comments.CommentError
import com.mmc.merimusicplayer.data.model.comments.CommentPage
import com.mmc.merimusicplayer.data.model.comments.CommentSort

internal fun requireNeteaseCommentProgress(result: CommentPage, sort: CommentSort, cursor: String?) {
    val stalledTimeCursor = sort == CommentSort.NEWEST &&
        (result.nextCursor.isNullOrBlank() || result.nextCursor == cursor)
    if (result.hasMore && (result.comments.isEmpty() || stalledTimeCursor)) {
        throw CommentApiException(200, CommentError.API, "NetEase comment pagination did not advance")
    }
}
