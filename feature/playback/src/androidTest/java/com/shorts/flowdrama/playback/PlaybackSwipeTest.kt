package com.shorts.flowdrama.playback

import androidx.activity.ComponentActivity
import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shorts.flowdrama.designsystem.FlowTheme
import com.shorts.flowdrama.domain.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackSwipeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun keepVisible() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            compose.activity.setShowWhenLocked(true)
            compose.activity.setTurnScreenOn(true)
        }
    }

    private val show = Show("show", mapOf("en" to "Show"), emptyMap(), emptyMap(), "", 3, "", 0, emptyList())
    private val episodes = listOf(
        Episode(1, true, 60, 0, emptyMap()),
        Episode(2, true, 60, 0, emptyMap()),
        Episode(3, false, 60, 0, emptyMap()),
    )

    @Test fun swipeUpMovesNextSwipeDownMovesBackAndLockedRequiresUnlock() {
        var selected = 1
        var locked = 0
        compose.setContent {
            var current by remember { mutableIntStateOf(1) }
            FlowTheme("en") {
                PlaybackScreen(
                    show = show,
                    state = PlaybackState(episodes, episodes[current - 1], loading = false),
                    preferences = Preferences(), defaultLanguage = "en", history = null,
                    onBack = {}, onEpisode = { selected = it.number; current = it.number },
                    onPreferences = {}, onProgress = { _, _, _ -> },
                    onLocked = { locked = it.number },
                )
            }
        }
        val surface = compose.onNodeWithTag("playback:video-surface")
        surface.performTouchInput { swipeUp() }
        compose.runOnIdle { assertEquals(2, selected) }
        surface.performTouchInput { swipeUp() }
        compose.runOnIdle { assertEquals(3, locked); assertEquals(2, selected) }
        surface.performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(1, selected) }
    }

    @Test fun swipeDoesNotChangeEpisodeWhileBlockedByAd() {
        var selected = 1
        compose.setContent {
            FlowTheme("en") {
                PlaybackScreen(
                    show = show,
                    state = PlaybackState(episodes, episodes.first(), loading = false),
                    preferences = Preferences(), defaultLanguage = "en", history = null,
                    onBack = {}, onEpisode = { selected = it.number },
                    onPreferences = {}, onProgress = { _, _, _ -> }, onLocked = {},
                    blockedByAd = true,
                )
            }
        }
        compose.onNodeWithTag("playback:video-surface").performTouchInput { swipeUp() }
        compose.runOnIdle { assertEquals(1, selected) }
    }
    @Test fun externallySelectedEpisodeMovesPagerWithoutTriggeringUnlock() {
        val current = mutableIntStateOf(1)
        var locked = 0
        var callbacks = 0
        compose.setContent {
            FlowTheme("en") {
                PlaybackScreen(
                    show = show,
                    state = PlaybackState(episodes, episodes[current.intValue - 1], loading = false),
                    preferences = Preferences(), defaultLanguage = "en", history = null,
                    onBack = {}, onEpisode = { callbacks++; current.intValue = it.number },
                    onPreferences = {}, onProgress = { _, _, _ -> },
                    onLocked = { locked = it.number },
                )
            }
        }
        compose.runOnIdle { current.intValue = 2 }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, callbacks); assertEquals(0, locked) }
        compose.onNodeWithTag("playback:video-surface").performTouchInput { swipeUp() }
        compose.runOnIdle { assertEquals(3, locked); assertEquals(2, current.intValue) }
    }

    @Test fun episodeTitleAppearsAndBlankTitleUsesShowName() {
        val current = mutableIntStateOf(0)
        val items = listOf(episodes[0].copy(title = "The first clue"), episodes[1].copy(title = "  "))
        compose.setContent {
            FlowTheme("en") {
                PlaybackScreen(
                    show = show,
                    state = PlaybackState(items, items[current.intValue], loading = false),
                    preferences = Preferences(), defaultLanguage = "en", history = null,
                    onBack = {}, onEpisode = { current.intValue = items.indexOf(it) },
                    onPreferences = {}, onProgress = { _, _, _ -> }, onLocked = {},
                )
            }
        }
        compose.onNodeWithText("The first clue").assertExists()
        compose.runOnIdle { current.intValue = 1 }
        compose.waitForIdle()
        compose.onNodeWithText("The first clue").assertDoesNotExist()
        compose.onAllNodesWithText("Show").assertCountEquals(2)
    }

}
