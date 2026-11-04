package com.shorts.flowdrama.domain

/** Offline playback can continue from cache. Show an error only once playback stalls. */
enum class PlaybackStatus { Content, Buffering, Offline, Unavailable }

fun playbackStatus(online: Boolean, buffering: Boolean, failed: Boolean, offlineVisible: Boolean = false): PlaybackStatus = when {
    !online && (offlineVisible || buffering || failed) -> PlaybackStatus.Offline
    failed -> PlaybackStatus.Unavailable
    buffering -> PlaybackStatus.Buffering
    else -> PlaybackStatus.Content
}
