package com.shorts.flowdrama

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shorts.flowdrama.ads.*
import com.shorts.flowdrama.ads.compose.*
import com.shorts.flowdrama.ads.config.*
import com.shorts.flowdrama.designsystem.FlowTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InlineAdSlotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Before fun keepVisible() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            compose.activity.setShowWhenLocked(true)
            compose.activity.setTurnScreenOn(true)
        }
    }

    @Test fun sameHomePlacementSwitchesNativeToBannerAndBackAndCanBeDisabled() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = AdPlacements.HomeFeed
        val native = AdPlacement(name, AdFormat.Native,
            adId = "ca-app-pub-3940256099942544/2247696110",
            style = NativeAdStyle(ctaBackground = "#8400FF", ctaText = "#FFFFFF", ctaRadiusDp = 8))
        val source = TestSource(AdConfiguration(placements = mapOf(name to native)))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val ads = AdsController(context, "1.0.0", true, scope, R.xml.remote_config_defaults, source)
        lateinit var state: InlineAdState
        try {
            compose.setContent {
                state = rememberInlineAdState(ads, name)
                FlowTheme("en") { InlineAdSlot(state, "Ad · Sponsored") }
            }
            loaded(state = { state }, format = AdFormat.Native)
            compose.runOnIdle { assertEquals("#8400FF", state.native.placement?.style?.ctaBackground) }
            compose.onNodeWithTag("ad:$name").assertExists()
            compose.runOnIdle {
                source.state.value = source.state.value.copy(placements = mapOf(name to native.copy(
                    format = AdFormat.Banner, adId = "ca-app-pub-3940256099942544/9214589741")))
            }
            loaded(state = { state }, format = AdFormat.Banner)
            compose.runOnIdle { assertEquals(AdLoadStatus.Disabled, state.native.status) }
            compose.onNodeWithTag("ad:$name").assertExists()
            compose.runOnIdle { source.state.value = source.state.value.copy(placements = mapOf(name to native)) }
            loaded(state = { state }, format = AdFormat.Native)
            compose.runOnIdle { assertEquals(AdLoadStatus.Disabled, state.banner.status) }
            compose.runOnIdle { source.state.value = source.state.value.copy(enabled = false) }
            compose.waitUntil(5_000) { !state.native.enabled && !state.banner.enabled }
            compose.onNodeWithTag("ad:$name").assertDoesNotExist()
        } finally { scope.cancel() }
    }

    private fun loaded(state: () -> InlineAdState, format: AdFormat) {
        compose.waitUntil(60_000) { state().format == format && state().status == AdLoadStatus.Loaded }
        compose.runOnIdle { assertEquals(AdLoadStatus.Loaded, state().status) }
    }

    private class TestSource(config: AdConfiguration) : AdConfigurationSource {
        override val state = MutableStateFlow(config)
        override val invalidParameters = MutableStateFlow<Set<String>>(emptySet())
        override fun refresh() = Unit
    }
}
