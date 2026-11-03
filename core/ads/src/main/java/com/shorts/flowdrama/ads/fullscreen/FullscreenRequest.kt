package com.shorts.flowdrama.ads.fullscreen

import com.shorts.flowdrama.ads.config.*

internal data class FullscreenRequest(val name: String, val id: String, val format: AdFormat)

/** A null requiredFormat follows Remote Config; typed APIs keep their explicit format contract. */
internal fun resolveFullscreenRequest(
    config: AdConfiguration, version: String, name: String, requiredFormat: AdFormat? = null,
): FullscreenRequest? {
    val place = config.placements[name] ?: return null
    if (!config.allows(name, version) || place.format == AdFormat.Native || place.format == AdFormat.Banner) return null
    if (requiredFormat != null && place.format != requiredFormat) return null
    return FullscreenRequest(name, config.unitId(place), place.format)
}

internal fun AdFormat.canGrantReward() = this == AdFormat.Rewarded || this == AdFormat.RewardedInterstitial
