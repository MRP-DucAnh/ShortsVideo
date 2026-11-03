package com.shorts.flowdrama.ads.fullscreen

import com.shorts.flowdrama.ads.config.*
import org.junit.Assert.*
import org.junit.Test

class FullscreenRequestTest {
    private val name = "onboarding_step4_notifications_complete"
    private fun config(format: AdFormat, id: String = "ca-app-pub-1/1") =
        AdConfiguration(placements = mapOf(name to AdPlacement(name, format, adId = id)))

    @Test fun onePlacementCanSwitchBetweenInterstitialAndBothRewardFormats() {
        listOf(AdFormat.Interstitial, AdFormat.Rewarded, AdFormat.RewardedInterstitial,
            AdFormat.Interstitial).forEach { format ->
            val request = requireNotNull(resolveFullscreenRequest(config(format), "1", name))
            assertEquals(name, request.name)
            assertEquals(format, request.format)
        }
    }

    @Test fun changingFormatOrIdMakesPreviouslyCachedRequestIncompatible() {
        val original = resolveFullscreenRequest(config(AdFormat.Rewarded), "1", name)
        val changedFormat = resolveFullscreenRequest(config(AdFormat.Interstitial), "1", name)
        val changedId = resolveFullscreenRequest(config(AdFormat.Rewarded, "ca-app-pub-2/2"), "1", name)
        assertNotEquals(original, changedFormat)
        assertNotEquals(original, changedId)
    }

    @Test fun fixedFormatApisStillRejectDifferentFormatsAndInlineFormatsAreNotFullscreen() {
        assertNull(resolveFullscreenRequest(config(AdFormat.Rewarded), "1", name, AdFormat.Interstitial))
        assertNull(resolveFullscreenRequest(config(AdFormat.Native), "1", name))
        assertNull(resolveFullscreenRequest(config(AdFormat.Banner), "1", name))
        assertNull(resolveFullscreenRequest(config(AdFormat.Rewarded).copy(enabled = false), "1", name))
    }

    @Test fun onlyActualRewardFormatsCanGrantRewards() {
        assertFalse(AdFormat.Interstitial.canGrantReward())
        assertFalse(AdFormat.AppOpen.canGrantReward())
        assertTrue(AdFormat.Rewarded.canGrantReward())
        assertTrue(AdFormat.RewardedInterstitial.canGrantReward())
    }
}
