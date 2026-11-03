package com.shorts.flowdrama.ads.fullscreen

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.*
import com.google.android.gms.ads.interstitial.*
import com.google.android.gms.ads.rewarded.*
import com.google.android.gms.ads.rewardedinterstitial.*
import com.shorts.flowdrama.ads.AdReward
import com.shorts.flowdrama.ads.config.AdFormat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** SDK-specific adapters keep callback types out of screen code. */
internal class LoadedFullscreenAd(
    val setCallback: (FullScreenContentCallback) -> Unit,
    val show: (Activity, (AdReward) -> Unit) -> Unit,
)

internal suspend fun loadFullscreen(context: Context, id: String, format: AdFormat): LoadedFullscreenAd =
    suspendCancellableCoroutine { continuation ->
        fun loaded(ad: LoadedFullscreenAd) { if (continuation.isActive) continuation.resume(ad) }
        fun failed(error: LoadAdError) {
            if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Ad load ${error.code}: ${error.message}"))
        }
        val request = AdRequest.Builder().build()
        when (format) {
            AdFormat.Interstitial -> InterstitialAd.load(context, id, request, object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) = failed(error)
                override fun onAdLoaded(ad: InterstitialAd) = loaded(LoadedFullscreenAd(
                    { ad.fullScreenContentCallback = it }, { activity, _ -> ad.show(activity) }))
            })
            AdFormat.Rewarded -> RewardedAd.load(context, id, request, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) = failed(error)
                override fun onAdLoaded(ad: RewardedAd) = loaded(LoadedFullscreenAd(
                    { ad.fullScreenContentCallback = it }, { activity, reward ->
                        ad.show(activity) { reward(AdReward(it.type, it.amount)) }
                    }))
            })
            AdFormat.RewardedInterstitial -> RewardedInterstitialAd.load(context, id, request, object : RewardedInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) = failed(error)
                override fun onAdLoaded(ad: RewardedInterstitialAd) = loaded(LoadedFullscreenAd(
                    { ad.fullScreenContentCallback = it }, { activity, reward ->
                        ad.show(activity) { reward(AdReward(it.type, it.amount)) }
                    }))
            })
            AdFormat.AppOpen -> AppOpenAd.load(context, id, request, object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) = failed(error)
                override fun onAdLoaded(ad: AppOpenAd) = loaded(LoadedFullscreenAd(
                    { ad.fullScreenContentCallback = it }, { activity, _ -> ad.show(activity) }))
            })
            else -> continuation.resumeWithException(IllegalArgumentException("Not a fullscreen format: $format"))
        }
    }
