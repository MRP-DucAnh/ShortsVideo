package com.shorts.flowdrama.ads

import android.app.Activity
import android.content.Context
import androidx.annotation.XmlRes
import com.google.android.gms.ads.MobileAds
import com.shorts.flowdrama.ads.config.*
import com.shorts.flowdrama.ads.fullscreen.FullscreenAds
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Shared SDK/configuration entry point. Screens only choose a placement and a reusable component. */
class AdsController(
    context: Context,
    private val versionName: String,
    debug: Boolean,
    scope: CoroutineScope,
    @XmlRes defaultsResource: Int,
    configurationSource: AdConfigurationSource? = null,
) {
    private val repository = configurationSource ?: RemoteAdConfiguration(context.applicationContext, defaultsResource, debug)
    val config = repository.state
    val invalidConfigParameters = repository.invalidParameters
    private val mutableReady = MutableStateFlow(false)
    val ready = mutableReady.asStateFlow()
    private val fullscreen = FullscreenAds(context.applicationContext, scope, config, ready, versionName, ::track)
    val isFullscreenShowing = fullscreen.showing
    val fullscreenStatus = fullscreen.status
    private val mutableEvents = MutableSharedFlow<AdPlacementEvent>(extraBufferCapacity = 64)
    val events = mutableEvents.asSharedFlow()

    internal fun track(placement: String, format: AdFormat, type: AdPlacementEventType) {
        val place = config.value.placements[placement] ?: return
        if (place.format != format || !allowed(placement)) return
        if ((type == AdPlacementEventType.Click && place.trackingClick) ||
            (type == AdPlacementEventType.Impression && place.trackingShow)) {
            mutableEvents.tryEmit(AdPlacementEvent(placement, format, type))
        }
    }

    init {
        scope.launch(Dispatchers.IO) {
            runCatching { MobileAds.initialize(context.applicationContext) { mutableReady.value = true } }
        }
    }

    fun allowed(placement: String) = config.value.allows(placement, versionName)
    fun refresh() = repository.refresh()
    fun preload(placement: String) = fullscreen.preload(placement)

    /** Resolve the format and unit ID from the current placement at show time. */
    fun showFullscreenAd(
        activity: Activity, placement: String,
        onReward: (AdReward) -> Unit = {}, onComplete: (AdShowResult) -> Unit = {},
    ) = fullscreen.show(activity, placement, null, onReward, onComplete)

    fun showInterstitial(activity: Activity, placement: String, onComplete: (AdShowResult) -> Unit = {}) =
        fullscreen.show(activity, placement, AdFormat.Interstitial, {}, onComplete)

    /** Reward is delivered only by the SDK earned-reward callback, never on dismissal. */
    fun showRewarded(activity: Activity, placement: String, onReward: (AdReward) -> Unit,
        onComplete: (AdShowResult) -> Unit = {}) =
        fullscreen.show(activity, placement, AdFormat.Rewarded, onReward, onComplete)

    /** Use RewardedInterstitialPrompt (or your own reward/skip intro) before calling this. */
    fun showRewardedInterstitial(activity: Activity, placement: String, onReward: (AdReward) -> Unit,
        onComplete: (AdShowResult) -> Unit = {}) =
        fullscreen.show(activity, placement, AdFormat.RewardedInterstitial, onReward, onComplete)

    /** Explicit placement: the library never inserts app-open ads into navigation automatically. */
    fun showAppOpen(activity: Activity, placement: String = AdPlacements.SplashAppOpen,
        onComplete: (AdShowResult) -> Unit = {}) =
        fullscreen.show(activity, placement, AdFormat.AppOpen, {}, onComplete)
}
