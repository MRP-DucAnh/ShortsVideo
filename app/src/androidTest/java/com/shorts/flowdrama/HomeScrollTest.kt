package com.shorts.flowdrama

import androidx.compose.ui.test.*
import androidx.activity.ComponentActivity
import android.view.WindowManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shorts.flowdrama.data.JsonCatalog
import com.shorts.flowdrama.designsystem.FlowTheme
import com.shorts.flowdrama.home.HomeScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScrollTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun keepTestWindowVisible() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            compose.activity.setShowWhenLocked(true)
            compose.activity.setTurnScreenOn(true)
        }
    }

    @Test
    fun adSlotKeepsItsHeightWhenScrolledOutAndBack() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as FlowDramaApplication
        val catalog = JsonCatalog().catalog(
            context.assets.open("fixtures/catalog.json").bufferedReader().use { it.readText() }
        )
        compose.setContent {
            FlowTheme("en") {
                HomeScreen(catalog, emptyList(), application.container.ads, {}, {}, {}, {})
            }
        }
        val feed = compose.onNodeWithTag("home:feed")
        feed.performScrollToNode(hasTestTag("ad:home_feed_after_continue_watching"))
        val initial = compose.onNodeWithTag("ad:home_feed_after_continue_watching").fetchSemanticsNode().size.height
        repeat(3) {
            feed.performScrollToIndex(0)
            feed.performScrollToNode(hasTestTag("ad:home_feed_after_continue_watching"))
            val height = compose.onNodeWithTag("ad:home_feed_after_continue_watching").fetchSemanticsNode().size.height
            assertEquals(initial, height)
        }
    }
}
