package com.shorts.flowdrama.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackStatusTest {
    @Test fun connectedStallShowsBuffering() {
        assertEquals(PlaybackStatus.Buffering, playbackStatus(true, true, false))
    }
    @Test fun lostInternetWhileStalledOrFailedShowsOffline() {
        assertEquals(PlaybackStatus.Offline, playbackStatus(false, true, false))
        assertEquals(PlaybackStatus.Offline, playbackStatus(false, false, true))
    }
    @Test fun cachedPlaybackContinuesAfterLosingInternet() {
        assertEquals(PlaybackStatus.Content, playbackStatus(false, false, false))
    }
    @Test fun reconnectChangesOfflineIntoBufferingUntilPlaybackResumes() {
        assertEquals(PlaybackStatus.Offline, playbackStatus(false, true, false))
        assertEquals(PlaybackStatus.Buffering, playbackStatus(true, true, false))
        assertEquals(PlaybackStatus.Content, playbackStatus(true, false, false))
    }
    @Test fun serverOrDecoderErrorWithInternetIsNotReportedAsOffline() {
        assertEquals(PlaybackStatus.Unavailable, playbackStatus(true, false, true))
    }
    @Test fun offlineOverlayStaysVisibleWhenPlayerBecomesReadyWithoutInternet() {
        assertEquals(PlaybackStatus.Offline, playbackStatus(false, false, false, offlineVisible = true))
    }
    @Test fun offlineOverlayOnlyClearsAfterInternetReturns() {
        assertEquals(PlaybackStatus.Buffering, playbackStatus(true, true, false, offlineVisible = true))
        assertEquals(PlaybackStatus.Content, playbackStatus(true, false, false, offlineVisible = true))
    }
}
