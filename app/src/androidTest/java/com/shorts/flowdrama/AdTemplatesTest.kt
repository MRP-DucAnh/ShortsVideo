package com.shorts.flowdrama

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.activity.ComponentActivity
import android.view.WindowManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shorts.flowdrama.ads.*
import com.shorts.flowdrama.ads.config.*
import com.shorts.flowdrama.ads.banner.*
import com.shorts.flowdrama.ads.nativead.*
import com.shorts.flowdrama.designsystem.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdTemplatesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun keepTestWindowVisible() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            compose.activity.setShowWhenLocked(true)
            compose.activity.setTurnScreenOn(true)
        }
    }

    @Test fun allNativeTemplatesRenderAtTheirDeclaredHeight() {
        var template by mutableStateOf(NativeTemplate.Small)
        compose.setContent {
            FlowTheme("en") {
                Box(Modifier.fillMaxSize().background(Sunset.Background).padding(16.dp)) {
                    NativeAdPreview(template, "Ad · Preview", Modifier.testTag("preview"))
                }
            }
        }
        NativeTemplate.entries.forEach { next ->
            compose.runOnIdle { template = next }
            compose.onNodeWithTag("preview").assertHeightIsEqualTo(next.heightDp.dp)
            screenshot(next.key)
        }
    }

    @Test fun realNativeAdKeepsTheSameSlotHeightBeforeAndAfterLoading() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FlowDramaApplication
        lateinit var state: NativeAdState
        compose.setContent {
            state = rememberNativeAdState(application.container.ads, AdPlacements.HomeFeed)
            FlowTheme("en") { NativeAdCard(state, "Ad · Sponsored") }
        }
        compose.waitUntil(5_000) { state.placement != null && state.enabled }
        val initialHeight = compose.onNodeWithTag("ad:home_feed_after_continue_watching").fetchSemanticsNode().size.height
        compose.waitUntil(60_000) { state.status == AdLoadStatus.Loaded || state.status == AdLoadStatus.Failed }
        compose.runOnIdle { assertEquals(AdLoadStatus.Loaded, state.status) }
        assertEquals(initialHeight, compose.onNodeWithTag("ad:home_feed_after_continue_watching").fetchSemanticsNode().size.height)
        screenshot("native_loaded")
    }

    @Test fun realBannerKeepsTheSameSlotHeightAfterLoading() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FlowDramaApplication
        lateinit var state: BannerAdState
        compose.setContent {
            state = rememberBannerAdState(application.container.ads, AdPlacements.LibraryBottom)
            FlowTheme("en") { BannerAd(state) }
        }
        compose.waitUntil(5_000) { state.enabled }
        val initialHeight = compose.onNodeWithTag("ad:library_recently_watched_bottom").fetchSemanticsNode().size.height
        compose.waitUntil(60_000) { state.status == AdLoadStatus.Loaded || state.status == AdLoadStatus.Failed }
        compose.runOnIdle { assertEquals(AdLoadStatus.Loaded, state.status) }
        assertEquals(initialHeight, compose.onNodeWithTag("ad:library_recently_watched_bottom").fetchSemanticsNode().size.height)
        screenshot("banner_loaded")
    }

    @Test fun allFullscreenFormatsLoadUsingConfiguredIds() = runBlocking {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as FlowDramaApplication
        val ads = application.container.ads
        val names = listOf(AdPlacements.OnboardingComplete, AdPlacements.DetailEpisodeUnlockRewarded,
            AdPlacements.DetailEpisodeUnlockRewardedInterstitial, AdPlacements.SplashAppOpen)
        withTimeout(60_000) {
            ads.ready.first { it }
            names.forEach(ads::preload)
            ads.fullscreenStatus.first { statuses -> names.all { statuses[it] == AdLoadStatus.Loaded } }
            Unit
        }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "ad-template-review/$name.png")
        file.parentFile!!.mkdirs()
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
