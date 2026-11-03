package com.shorts.flowdrama.ads.fullscreen

import com.shorts.flowdrama.ads.config.*
import org.junit.Assert.*
import org.junit.Test

class AdDisplayLimiterTest {
    @Test fun intervalAndSessionCapUseActualImpressions() {
        var now = 0L
        val limiter = AdDisplayLimiter { now }
        val format = AdFormat.Interstitial
        val policy = AdFormatPolicy(intervalSeconds = 15, adsPerSession = 2, sessionSeconds = 60)
        assertTrue(limiter.allows(format, policy, false))
        limiter.record(format)
        assertFalse(limiter.allows(format, policy, false))
        now = 15_000
        assertTrue(limiter.allows(format, policy, false))
        limiter.record(format)
        now = 50_000
        assertFalse(limiter.allows(format, policy, false))
        now = 60_000
        assertTrue(limiter.allows(format, policy, false))
    }

    @Test fun ignoringIntervalDoesNotBypassSessionCapOrAffectOtherFormats() {
        val limiter = AdDisplayLimiter { 1_000L }
        val policy = AdFormatPolicy(intervalSeconds = 15, adsPerSession = 1)
        limiter.record(AdFormat.Interstitial)
        assertFalse(limiter.allows(AdFormat.Interstitial, policy, true))
        assertTrue(limiter.allows(AdFormat.Rewarded, policy, false))
    }
}
