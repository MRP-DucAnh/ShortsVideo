package com.shorts.flowdrama.ads.fullscreen

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.shorts.flowdrama.ads.*
import com.shorts.flowdrama.ads.config.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class FullscreenAds(
    private val context: Context,
    private val scope: CoroutineScope,
    private val configuration: StateFlow<AdConfiguration>,
    private val ready: StateFlow<Boolean>,
    private val version: String,
    private val track: (String, AdFormat, AdPlacementEventType) -> Unit,
) {
    private data class Cached(val token: FullscreenRequest, val ad: LoadedFullscreenAd, val loadedAt: Long)
    private val cache = mutableMapOf<String, Cached>()
    private val jobs = mutableMapOf<String, Job>()
    private val loadingTokens = mutableMapOf<String, FullscreenRequest>()
    private val limiter = AdDisplayLimiter(SystemClock::elapsedRealtime)
    private val mutableShowing = MutableStateFlow(false)
    val showing = mutableShowing.asStateFlow()
    private val mutableStatus = MutableStateFlow<Map<String, AdLoadStatus>>(emptyMap())
    val status = mutableStatus.asStateFlow()

    init {
        scope.launch(Dispatchers.Main.immediate) {
            configuration.collect {
                cache.keys.toList().filter { cache[it]?.token != token(it) }.forEach {
                    cache.remove(it)
                    setStatus(it, if (token(it) == null) AdLoadStatus.Disabled else AdLoadStatus.Idle)
                }
                jobs.keys.toList().filter { loadingTokens[it] != token(it) }.forEach {
                    jobs.remove(it)?.cancel()
                    loadingTokens.remove(it)
                    setStatus(it, if (token(it) == null) AdLoadStatus.Disabled else AdLoadStatus.Idle)
                    if (token(it) != null) preload(it)
                }
            }
        }
    }

    private fun token(name: String) = resolveFullscreenRequest(configuration.value, version, name)

    fun preload(name: String) {
        scope.launch(Dispatchers.Main.immediate) {
            val requested = token(name)
            if (requested == null) { setStatus(name, AdLoadStatus.Disabled); return@launch }
            if (jobs[name]?.isActive == true) return@launch
            val existing = cache[name]
            if (existing?.token == requested && !expired(existing)) return@launch
            cache.remove(name)
            loadingTokens[name] = requested
            jobs[name] = launch {
                setStatus(name, AdLoadStatus.Loading)
                try {
                    ready.first { it }
                    if (token(name) != requested) return@launch
                    val ad = loadWithRetry(configuration.value.policy(requested.format)) {
                        loadFullscreen(context, requested.id, requested.format)
                    }
                    if (token(name) == requested) {
                        cache[name] = Cached(requested, ad, SystemClock.elapsedRealtime())
                        setStatus(name, AdLoadStatus.Loaded)
                    } else setStatus(name, AdLoadStatus.Idle)
                } catch (cancelled: CancellationException) {
                    if (cancelled is TimeoutCancellationException) setStatus(name, AdLoadStatus.Failed)
                    else throw cancelled
                } catch (_: Exception) { setStatus(name, AdLoadStatus.Failed) }
            }
        }
    }

    fun show(
        activity: Activity, name: String, expectedFormat: AdFormat?,
        onReward: (AdReward) -> Unit, onComplete: (AdShowResult) -> Unit,
    ) {
        scope.launch(Dispatchers.Main.immediate) {
            val requested = resolveFullscreenRequest(configuration.value, version, name, expectedFormat)
            when {
                requested == null -> { onComplete(AdShowResult.Disabled); return@launch }
                activity.isFinishing || activity.isDestroyed -> { onComplete(AdShowResult.ActivityUnavailable); return@launch }
                showing.value -> { onComplete(AdShowResult.Busy); return@launch }
            }
            val format = requested.format
            val place = configuration.value.placements.getValue(name)
            if (!limiter.allows(format, configuration.value.policy(format), place.ignoreInterval)) {
                onComplete(AdShowResult.RateLimited); return@launch
            }
            val cached = cache.remove(name)
            if (cached == null || cached.token != requested || expired(cached)) {
                preload(name)
                onComplete(AdShowResult.NotReady)
                return@launch
            }
            mutableShowing.value = true
            setStatus(name, AdLoadStatus.Idle)
            var finished = false
            var rewarded = false
            var recorded = false
            fun complete(result: AdShowResult) {
                if (finished) return
                finished = true
                mutableShowing.value = false
                if (configuration.value.placements[name]?.autoReload == true) preload(name)
                onComplete(result)
            }
            cached.ad.setCallback(object : FullScreenContentCallback() {
                override fun onAdClicked() = track(name, format, AdPlacementEventType.Click)
                override fun onAdImpression() = track(name, format, AdPlacementEventType.Impression)
                override fun onAdShowedFullScreenContent() {
                    if (!recorded) { recorded = true; limiter.record(format) }
                }
                override fun onAdDismissedFullScreenContent() = complete(AdShowResult.Dismissed)
                override fun onAdFailedToShowFullScreenContent(error: AdError) = complete(AdShowResult.Failed)
            })
            runCatching {
                cached.ad.show(activity) {
                    if (format.canGrantReward() && !rewarded) { rewarded = true; onReward(it) }
                }
            }.onFailure { complete(AdShowResult.Failed) }
        }
    }

    private fun expired(cached: Cached): Boolean {
        val lifetime = if (cached.token.format == AdFormat.AppOpen) 4 * 60 * 60_000L else 60 * 60_000L
        return SystemClock.elapsedRealtime() - cached.loadedAt >= lifetime
    }

    private fun setStatus(name: String, state: AdLoadStatus) { mutableStatus.value = mutableStatus.value + (name to state) }
}
