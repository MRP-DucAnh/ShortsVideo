package com.shorts.flowdrama.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.shorts.flowdrama.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject

object TestAds {
    const val Banner = "ca-app-pub-3940256099942544/9214589741"
    const val Native = "ca-app-pub-3940256099942544/2247696110"
    const val Interstitial = "ca-app-pub-3940256099942544/1033173712"
}

class AdsController(
    context: Context,
    private val versionName: String,
    debug: Boolean,
    scope: CoroutineScope,
) {
    private val app = context.applicationContext
    private val policy = AdsPolicy()
    private val mutableConfig = MutableStateFlow(AdsConfig())
    val config = mutableConfig.asStateFlow()
    private val mutableReady = MutableStateFlow(false)
    val ready = mutableReady.asStateFlow()
    private var interstitial: InterstitialAd? = null
    private val remote = FirebaseRemoteConfig.getInstance()

    init {
        remote.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (debug) 0 else 3600)
                .setFetchTimeoutInSeconds(8)
                .build()
        )
        remote.setDefaultsAsync(mapOf("ads_config" to DEFAULT)).addOnCompleteListener {
            applyConfig()
            refresh()
        }
        scope.launch(Dispatchers.IO) {
            runCatching {
                MobileAds.initialize(app) {
                    mutableReady.value = true
                    loadInterstitial()
                }
            }
        }
    }

    fun allowed(placement: String) = policy.allows(config.value, versionName, placement)

    private fun applyConfig() {
        mutableConfig.value =
            runCatching { parse(remote.getString("ads_config")) }.getOrDefault(AdsConfig())
        if (!allowed("interstitial_onboarding")) interstitial = null
    }

    fun refresh() {
        remote.fetchAndActivate().addOnCompleteListener { applyConfig() }
    }

    private fun loadInterstitial() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (!allowed("interstitial_onboarding")) return@post
            runCatching {
                InterstitialAd.load(
                    app,
                    TestAds.Interstitial,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            if (allowed("interstitial_onboarding")) interstitial = ad
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            interstitial = null
                        }
                    },
                )
            }
        }
    }

    /** Caller must be at a non-playback transition. Never wait for an unavailable ad. */
    fun finishOnboarding(activity: Activity, proceed: () -> Unit) {
        val ad = interstitial
        interstitial = null
        if (!allowed("interstitial_onboarding") || ad == null || activity.isFinishing) {
            proceed()
            return
        }
        var completed = false
        fun complete() {
            if (!completed) {
                completed = true
                proceed()
            }
        }
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() = complete()

                override fun onAdFailedToShowFullScreenContent(error: AdError) = complete()
            }
        runCatching { ad.show(activity) }.onFailure { complete() }
    }

    companion object {
        const val DEFAULT =
            """{"enable":true,"disabled_versions":[],"placements":{"interstitial_onboarding":{"enable":true},"native_home":{"enable":true},"banner_library":{"enable":true}}}"""

        fun parse(raw: String): AdsConfig {
            val root = JSONObject(raw)
            val disabled = root.optJSONArray("disabled_versions")
            val placements = root.getJSONObject("placements")
            return AdsConfig(
                root.getBoolean("enable"),
                if (disabled == null) emptySet()
                else (0 until disabled.length()).map { disabled.getString(it) }.toSet(),
                placements.keys().asSequence().associateWith {
                    placements.getJSONObject(it).optBoolean("enable", false)
                },
            )
        }
    }
}
