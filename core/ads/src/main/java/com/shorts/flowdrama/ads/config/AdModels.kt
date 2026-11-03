package com.shorts.flowdrama.ads.config

enum class AdFormat(val key: String) {
    Banner("banner"), Native("native"), Interstitial("interstitial"),
    Rewarded("rewarded"), RewardedInterstitial("rewarded_interstitial"), AppOpen("app_open");

    companion object {
        fun fromKey(key: String) = entries.find { it.key == key }
    }
}

enum class BannerSize(val key: String) {
    Anchored("anchored"), AnchoredLarge("anchored_large"), Inline("inline"),
    StandardMedium("standard_medium"), StandardLarge("standard_large");

    companion object {
        fun fromKey(key: String) = entries.find { it.key == key } ?: Anchored
    }
}

enum class CtaPosition { Top, Bottom, Right, RightTop }
enum class NativeLayout { Compact, Long, Popup, MediaTop, MediaLeft, MediaRight, Full, InterstitialV1, InterstitialV2 }

/** Keys match the reference project's native_template_size values. Heights include the card only. */
enum class NativeTemplate(
    val key: String,
    val heightDp: Int,
    val layout: NativeLayout,
    val cta: CtaPosition = CtaPosition.Bottom,
) {
    Small("small", 148, NativeLayout.Compact, CtaPosition.Right),
    SmallCtaTop("small_cta_top", 168, NativeLayout.Compact, CtaPosition.Top),
    SmallCtaBottom("small_cta_bottom", 168, NativeLayout.Compact),
    SmallCtaRight("small_cta_right", 120, NativeLayout.Compact, CtaPosition.Right),
    MiniCtaRight("mini_cta_right", 88, NativeLayout.Compact, CtaPosition.Right),
    SmallLong("small_long", 192, NativeLayout.Long),
    SmallForPopup("small_for_popup", 216, NativeLayout.Popup),
    Medium("medium", 320, NativeLayout.MediaTop),
    MediumCtaTop("medium_cta_top", 320, NativeLayout.MediaTop, CtaPosition.Top),
    MediumCtaBottom("medium_cta_bottom", 320, NativeLayout.MediaTop),
    MediumCtaRightTop("medium_cta_right_top", 300, NativeLayout.MediaTop, CtaPosition.RightTop),
    MediumCtaRight("medium_cta_right", 300, NativeLayout.MediaTop, CtaPosition.Right),
    MediumMediaRight("medium_media_right", 220, NativeLayout.MediaRight),
    MediumMediaLeft("medium_media_left", 220, NativeLayout.MediaLeft),
    FullCtaBottom("full_cta_bottom", 520, NativeLayout.Full),
    FullCtaTop("full_cta_top", 520, NativeLayout.Full, CtaPosition.Top),
    FullCtaRight("full_cta_right", 520, NativeLayout.Full, CtaPosition.Right),
    FullInterstitialV1("full_interstitial_v1", 560, NativeLayout.InterstitialV1),
    FullInterstitialV2("full_interstitial_v2", 560, NativeLayout.InterstitialV2, CtaPosition.Top);

    companion object {
        fun fromKey(key: String) = entries.find { it.key == key } ?: SmallCtaBottom
    }
}

data class NativeAdStyle(
    val background: String = "#241812",
    val headline: String = "#FFFFFF",
    val body: String = "#C9BCAE",
    val ctaBackground: String = "#FF7A3C",
    val ctaText: String = "#1A0E05",
    val border: String = "#4A3325",
    val radiusDp: Int = 16,
    val ctaRadiusDp: Int = 24,
    val fullBackground: String = "#160F0A",
    val mediaBackground: String = "#160F0A",
    val ctaBorder: String = "#FF7A3C",
)

data class AdPlacement(
    val name: String,
    val format: AdFormat,
    val idKey: String = format.key,
    val adId: String? = null,
    val enabled: Boolean = true,
    val template: NativeTemplate = NativeTemplate.SmallCtaBottom,
    val bannerSize: BannerSize = BannerSize.Anchored,
    val collapsible: Boolean = false,
    val autoReload: Boolean = false,
    val ignoreInterval: Boolean = false,
    val trackingClick: Boolean = false,
    val trackingShow: Boolean = false,
    val style: NativeAdStyle = NativeAdStyle(),
)

data class AdFormatPolicy(
    val retryEnabled: Boolean = true,
    val maxRetries: Int = 2,
    val retrySeconds: List<Long> = listOf(1, 2, 3),
    val intervalSeconds: Long = 0,
    val adsPerSession: Int = Int.MAX_VALUE,
    val sessionSeconds: Long = 600,
) {
    fun retryDelayMillis(attempt: Int) = (retrySeconds.getOrNull(attempt) ?: retrySeconds.lastOrNull() ?: 1) * 1000
}

data class AdConfiguration(
    val enabled: Boolean = true,
    val disabledVersions: Set<String> = emptySet(),
    val placementSwitches: Map<String, Boolean> = emptyMap(),
    val ids: Map<String, String> = emptyMap(),
    val placements: Map<String, AdPlacement> = emptyMap(),
    val policies: Map<AdFormat, AdFormatPolicy> = emptyMap(),
) {
    fun unitId(placement: AdPlacement): String = placement.adId ?: ids[placement.idKey].orEmpty()

    fun allows(name: String, version: String): Boolean {
        val placement = placements[name] ?: return false
        return enabled && version !in disabledVersions && placement.enabled &&
            placementSwitches[name] != false && unitId(placement).isNotBlank()
    }

    fun policy(format: AdFormat) = policies[format] ?: AdFormatPolicy()
}

/** Stable names belong to the app's placement catalog, never to a particular renderer. */
object AdPlacements {
    const val HomeFeed = "home_feed_after_continue_watching"
    const val LibraryBottom = "library_recently_watched_bottom"
    const val PlayerEpisodeUnlockRewarded = "player_episode_picker_unlock_rewarded"
    const val PlayerExit = "player_exit_to_previous_screen"
    const val OnboardingComplete = "onboarding_step4_notifications_complete"
    const val DetailEpisodeUnlockRewarded = "detail_episode_unlock_rewarded"
    const val DetailEpisodeUnlockRewardedInterstitial = "detail_episode_unlock_rewarded_interstitial"
    const val SplashAppOpen = "splash_app_open"
}
