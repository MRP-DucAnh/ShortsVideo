package com.shorts.flowdrama

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shorts.flowdrama.designsystem.FlowTheme
import com.shorts.flowdrama.detail.DetailScreen
import com.shorts.flowdrama.domain.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val show = Show("test", mapOf("en" to "A test drama"), mapOf("en" to "A short synopsis."),
        emptyMap(), "", 45, "completed", 1000, listOf("romance"))
    private val episodes = (1..45).map { Episode(it, it <= 6, 90, 0, emptyMap()) }
    private val progress = Progress("test", 5, 1000, 90000, 0, false)

    @Test fun rangesExpandInlineAndRouteLockedEpisodes() {
        var played = 0
        var locked = 0
        compose.setContent {
            FlowTheme("en") {
                DetailScreen(show, episodes, false, progress, false, {}, {}, {},
                    { played = it }, { locked = it.number }, watched = setOf(1, 2, 3, 4))
            }
        }
        compose.onNodeWithTag("detail:episode-preview").performScrollTo()
        compose.onNodeWithText("10", substring = false).assertExists()
        compose.onNodeWithText("11", substring = false).assertDoesNotExist()
        compose.onNodeWithText("21–40").assertDoesNotExist()
        compose.onNodeWithTag("detail:episode-picker").performScrollTo().performClick()
        compose.onNodeWithTag("detail:episode-ranges").assertExists()
        compose.onNodeWithText("21–40").performScrollTo().performClick()
        compose.onNodeWithText("21", substring = false).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(21, locked); assertEquals(0, played) }
        compose.onNodeWithText("21–40").assertExists()
        compose.onNodeWithTag("detail:episode-picker").performScrollTo().performClick()
        compose.onNodeWithTag("detail:episode-ranges").assertDoesNotExist()
        compose.onNodeWithText("30", substring = false).assertExists()
        compose.onNodeWithText("31", substring = false).assertDoesNotExist()
    }

    @Test fun continueAndSaveUseTheCurrentEpisodeAndBookmarkState() {
        var played = 0
        compose.setContent {
            var saved by remember { mutableStateOf(false) }
            FlowTheme("en") {
                DetailScreen(show, episodes, saved, progress, false, {}, {}, { saved = !saved },
                    { played = it }, {}, tags = listOf(Tag("romance", mapOf("en" to "Romance"), true, 0)))
            }
        }
        compose.onNodeWithText("Romance").assertExists()
        compose.onNodeWithText("Continue Ep. 5").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(5, played) }
        compose.onNodeWithContentDescription("Save").performClick()
        compose.onNodeWithContentDescription("Remove").assertExists()
    }
}
