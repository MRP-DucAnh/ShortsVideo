package com.shorts.flowdrama.ads.nativead

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.*
import com.shorts.flowdrama.ads.*
import com.shorts.flowdrama.ads.config.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Stable
class NativeAdState internal constructor(val placementName: String) {
    internal var generation by mutableIntStateOf(0)
    fun reload() { generation++ }
    var status by mutableStateOf(AdLoadStatus.Idle)
        internal set
    internal var ad by mutableStateOf<NativeAd?>(null)
    var placement by mutableStateOf<AdPlacement?>(null)
        internal set
    var enabled by mutableStateOf(false)
        internal set
}

/** Call at screen scope, above LazyColumn, so scrolling never owns the ad request. */
@Composable
fun rememberNativeAdState(controller: AdsController, placement: String): NativeAdState {
    val config by controller.config.collectAsStateWithLifecycle()
    val ready by controller.ready.collectAsStateWithLifecycle()
    val context = LocalContext.current.applicationContext
    val state = remember(controller, placement) { NativeAdState(placement) }
    val place = config.placements[placement]?.takeIf { it.format == AdFormat.Native }
    val enabled = place != null && controller.allowed(placement)
    val id = place?.let(config::unitId).orEmpty()
    val policy = config.policy(AdFormat.Native)
    SideEffect { state.placement = place; state.enabled = enabled }
    LaunchedEffect(state, ready, enabled, id, policy, state.generation) {
        state.ad?.destroy()
        state.ad = null
        if (!enabled) state.status = AdLoadStatus.Disabled
        else if (!ready) state.status = AdLoadStatus.Idle
        else {
            state.status = AdLoadStatus.Loading
            try {
                state.ad = loadWithRetry(policy) {
                    suspendCancellableCoroutine { continuation ->
                        AdLoader.Builder(context, id)
                            .withNativeAdOptions(NativeAdOptions.Builder()
                                .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build()).build())
                            .forNativeAd { ad ->
                                if (continuation.isActive) continuation.resume(ad) { _, cancelledAd, _ -> cancelledAd.destroy() } else ad.destroy()
                            }
                            .withAdListener(object : AdListener() {
                                override fun onAdClicked() = controller.track(placement, AdFormat.Native, AdPlacementEventType.Click)
                                override fun onAdImpression() = controller.track(placement, AdFormat.Native, AdPlacementEventType.Impression)
                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    if (continuation.isActive) continuation.resumeWithException(
                                        IllegalStateException("Native ad load ${error.code}"))
                                }
                            }).build().loadAd(AdRequest.Builder().build())
                    }
                }
                state.status = AdLoadStatus.Loaded
            } catch (cancelled: CancellationException) {
                if (cancelled is TimeoutCancellationException) state.status = AdLoadStatus.Failed
                else throw cancelled
            } catch (_: Exception) { state.status = AdLoadStatus.Failed }
        }
    }
    DisposableEffect(state) {
        onDispose { state.ad?.destroy(); state.ad = null }
    }
    return state
}
