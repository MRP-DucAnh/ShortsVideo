package com.shorts.flowdrama.ads.banner

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.shorts.flowdrama.ads.*
import com.shorts.flowdrama.ads.config.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Stable
class BannerAdState internal constructor(val placementName: String) {
    internal var generation by mutableIntStateOf(0)
    fun reload() { generation++ }
    var status by mutableStateOf(AdLoadStatus.Idle)
        internal set
    var enabled by mutableStateOf(false)
        internal set
    var heightDp by mutableIntStateOf(50)
        internal set
    internal var view by mutableStateOf<AdView?>(null)
}

/** Keep this state at screen scope, including when the banner is an item in a lazy list. */
@Composable
fun rememberBannerAdState(controller: AdsController, placement: String, widthDp: Int? = null): BannerAdState {
    val config by controller.config.collectAsStateWithLifecycle()
    val ready by controller.ready.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val width = widthDp ?: LocalConfiguration.current.screenWidthDp
    val state = remember(controller, placement) { BannerAdState(placement) }
    val place = config.placements[placement]?.takeIf { it.format == AdFormat.Banner }
    val enabled = place != null && controller.allowed(placement)
    val id = place?.let(config::unitId).orEmpty()
    val size = remember(context, width, place?.bannerSize) {
        when (place?.bannerSize) {
            // SDK 24.7 has no large adaptive API; keep this key compatible with a 320x100 banner.
            BannerSize.AnchoredLarge -> AdSize.LARGE_BANNER
            BannerSize.Inline -> AdSize.getInlineAdaptiveBannerAdSize(width, 100)
            BannerSize.StandardMedium -> AdSize.MEDIUM_RECTANGLE
            BannerSize.StandardLarge -> AdSize.LARGE_BANNER
            else -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width)
        }
    }
    // Inline adaptive sizes report height=0 before load, so reserve the requested maximum.
    val height = if (place?.bannerSize == BannerSize.Inline) 100 else
        (size.getHeightInPixels(context) / context.resources.displayMetrics.density).toInt().coerceAtLeast(50)
    SideEffect { state.enabled = enabled; state.heightDp = height }
    val policy = config.policy(AdFormat.Banner)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, ready, enabled, id, size, place?.collapsible, policy, state.generation) {
        state.view?.destroy()
        state.view = null
        if (!enabled) state.status = AdLoadStatus.Disabled
        else if (!ready) state.status = AdLoadStatus.Idle
        else {
            state.status = AdLoadStatus.Loading
            try {
                state.view = loadWithRetry(policy) {
                    suspendCancellableCoroutine { continuation ->
                        val view = AdView(context).apply { adUnitId = id; setAdSize(size) }
                        continuation.invokeOnCancellation { Handler(Looper.getMainLooper()).post { view.destroy() } }
                        view.adListener = object : AdListener() {
                            override fun onAdClicked() = controller.track(placement, AdFormat.Banner, AdPlacementEventType.Click)
                            override fun onAdImpression() = controller.track(placement, AdFormat.Banner, AdPlacementEventType.Impression)
                            override fun onAdLoaded() {
                                if (continuation.isActive) continuation.resume(view) else if (state.view !== view) view.destroy()
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                if (continuation.isActive) {
                                    view.destroy()
                                    continuation.resumeWithException(IllegalStateException("Banner load ${error.code}"))
                                }
                            }
                        }
                        val request = AdRequest.Builder()
                        if (place?.collapsible == true) request.addNetworkExtrasBundle(AdMobAdapter::class.java,
                            Bundle().apply { putString("collapsible", "bottom") })
                        try {
                            view.loadAd(request.build())
                        } catch (error: Exception) {
                            view.destroy()
                            if (continuation.isActive) continuation.resumeWithException(error)
                        }
                    }
                }
                state.status = AdLoadStatus.Loaded
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) state.view?.pause()
            } catch (cancelled: CancellationException) {
                if (cancelled is TimeoutCancellationException) state.status = AdLoadStatus.Failed
                else throw cancelled
            } catch (_: Exception) { state.status = AdLoadStatus.Failed }
        }
    }
    DisposableEffect(state, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> state.view?.resume()
                Lifecycle.Event.ON_PAUSE -> state.view?.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); state.view?.destroy(); state.view = null }
    }
    return state
}

@Composable
fun BannerAd(state: BannerAdState, modifier: Modifier = Modifier) {
    if (!state.enabled) return
    Box(modifier.fillMaxWidth().height(state.heightDp.dp).clipToBounds()
        .testTag("ad:${state.placementName}"), contentAlignment = Alignment.Center) {
        state.view?.let { adView ->
            key(adView) {
                AndroidView(
                    factory = {
                        (adView.parent as? ViewGroup)?.removeView(adView)
                        adView
                    },
                    modifier = Modifier.wrapContentSize(),
                    // The screen owns this AdView; list virtualization must not destroy it.
                    onRelease = { (it.parent as? ViewGroup)?.removeView(it) },
                )
            }
        }
    }
}
