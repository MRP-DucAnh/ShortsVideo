package com.shorts.flowdrama.ads

import android.graphics.Color
import android.widget.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.*

@Composable
fun LibraryBanner(controller: AdsController) {
    val config by controller.config.collectAsStateWithLifecycle()
    val ready by controller.ready.collectAsStateWithLifecycle()
    if (!ready || !controller.allowed("banner_library")) return
    val context = LocalContext.current
    val width = LocalConfiguration.current.screenWidthDp
    val view =
        remember(config, width) {
            AdView(context).apply {
                adUnitId = TestAds.Banner
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width))
            }
        }
    var loaded by remember(view) { mutableStateOf(false) }
    DisposableEffect(view) {
        view.adListener =
            object : AdListener() {
                override fun onAdLoaded() {
                    loaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loaded = false
                }
            }
        runCatching { view.loadAd(AdRequest.Builder().build()) }
        onDispose { view.destroy() }
    }
    if (loaded)
        AndroidView(
            factory = { view },
            modifier = Modifier.fillMaxWidth().height((view.adSize?.height ?: 50).dp),
        )
}

@Composable
fun HomeNative(controller: AdsController) {
    val config by controller.config.collectAsStateWithLifecycle()
    val ready by controller.ready.collectAsStateWithLifecycle()
    if (!ready || !controller.allowed("native_home")) return
    val context = LocalContext.current
    var native by remember(config) { mutableStateOf<NativeAd?>(null) }
    DisposableEffect(config) {
        var disposed = false
        var owned: NativeAd? = null
        val loader =
            AdLoader.Builder(context, TestAds.Native)
                .forNativeAd { ad ->
                    if (disposed || !controller.allowed("native_home")) ad.destroy()
                    else {
                        owned = ad
                        native = ad
                    }
                }
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            native = null
                        }
                    }
                )
                .build()
        runCatching { loader.loadAd(AdRequest.Builder().build()) }
        onDispose {
            disposed = true
            owned?.destroy()
        }
    }
    native?.let { ad ->
        AndroidView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            factory = { ctx ->
                NativeAdView(ctx).apply {
                    val layout =
                        LinearLayout(ctx).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(24, 18, 24, 18)
                            setBackgroundColor(Color.rgb(36, 24, 18))
                        }
                    val label =
                        TextView(ctx).apply {
                            text = "Ad · Sponsored"
                            setTextColor(Color.LTGRAY)
                            textSize = 10f
                        }
                    layout.addView(label)
                    val headline =
                        TextView(ctx).apply {
                            text = ad.headline
                            setTextColor(Color.WHITE)
                            textSize = 17f
                        }
                    layout.addView(headline)
                    headlineView = headline
                    val body =
                        TextView(ctx).apply {
                            text = ad.body
                            setTextColor(Color.LTGRAY)
                        }
                    layout.addView(body)
                    bodyView = body
                    val button = Button(ctx).apply { text = ad.callToAction }
                    layout.addView(button)
                    callToActionView = button
                    addView(layout)
                    setNativeAd(ad)
                }
            },
        )
    }
}
