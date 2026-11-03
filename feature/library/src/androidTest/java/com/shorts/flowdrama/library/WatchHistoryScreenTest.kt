package com.shorts.flowdrama.library

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shorts.flowdrama.designsystem.FlowTheme
import com.shorts.flowdrama.domain.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WatchHistoryScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun keepVisible() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            compose.activity.setShowWhenLocked(true)
            compose.activity.setTurnScreenOn(true)
        }
    }

    @Test fun filtersOpensAndRemovesSpecificEpisodes() {
        val show = Show("s", mapOf("en" to "Drama"), emptyMap(), emptyMap(), "", 5, "", 0, emptyList())
        val catalog = Catalog("en", listOf("en"), emptyList(), "s", listOf(show))
        val history = listOf(
            Progress("s", 1, 30, 100, 1, watched = false),
            Progress("s", 2, 100, 100, 2, watched = true),
            Progress("s", 3, 10, 100, 3, watched = true),
        )
        var opened: Pair<String, Int>? = null
        var removed: Pair<String, Int>? = null
        compose.setContent {
            FlowTheme("en") {
                HistoryScreen(catalog, history, onBack = {},
                    onEpisode = { id, number -> opened = id to number },
                    onRemove = { id, number -> removed = id to number }, onClear = {}, onBrowse = {})
            }
        }
        compose.onNodeWithText("Ep. 1 / 5 · 1 min left").assertExists()
        compose.onNodeWithText("Ep. 2 / 5").assertDoesNotExist()
        compose.onNodeWithText("Finished").performClick()
        compose.onNodeWithText("Ep. 1 / 5 · 1 min left").assertDoesNotExist()
        compose.onNodeWithText("Ep. 2 / 5").assertExists().performClick()
        compose.onNodeWithText("Ep. 3 / 5").assertExists()
        compose.runOnIdle { assertEquals("s" to 2, opened) }
        compose.onNodeWithText("Edit").performClick()
        compose.onAllNodesWithContentDescription("Remove").onFirst().performClick()
        compose.runOnIdle { assertEquals("s" to 3, removed) }
    }
}
