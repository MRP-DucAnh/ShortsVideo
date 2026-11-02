package com.shorts.flowdrama

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shorts.flowdrama.data.JsonCatalog
import com.shorts.flowdrama.domain.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlowDramaJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitText(text: String) {
        compose.waitUntil(25000) {
            runCatching { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
                .getOrDefault(false)
        }
    }

    private fun snapshot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(400)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file =
            File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots/$name.png")
        file.parentFile!!.mkdirs()
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            file.outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }

    @Test
    fun onboardingDiscoveryLibraryAndSettings() {
        waitText("Your language")
        snapshot("SCR-0002")
        compose.onNodeWithText("Continue").performClick()
        waitText("Every episode open.\nThe story just flows.")
        snapshot("SCR-0003")
        compose.onNodeWithText("Step into the flow").performClick()
        waitText("How should tonight feel?")
        snapshot("SCR-0004")
        compose.onNodeWithText("Swoon").performClick()
        compose.onNodeWithText("Continue").performClick()
        waitText("Your flow runs three times a day")
        snapshot("SCR-0005")
        compose.onNodeWithText("Not now").performClick()
        waitText("Home")
        snapshot("SCR-0101")
        compose.onNodeWithText("Browse").performClick()
        waitText("Tap a poster · preview changes above")
        snapshot("SCR-0202")
        compose.onNodeWithContentDescription("Save").performClick()
        compose.onNodeWithText("Library").performClick()
        waitText("Watch history")
        snapshot("SCR-0401")
        compose.onNodeWithText("Saved").performClick()
        waitText("Saved")
        snapshot("SCR-0403")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithContentDescription("Search").performClick()
        waitText("Search dramas")
        snapshot("SCR-0201")
        compose.onNode(hasSetTextAction()).performTextInput("no-such-drama-xyz")
        waitText("No results")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Library").performClick()
        compose.onNodeWithText("Watch history").performClick()
        waitText("No watch history yet")
        snapshot("SCR-0402")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Settings").performClick()
        waitText("App language")
        snapshot("SCR-0501")
        compose.onNodeWithText("App language").performClick()
        waitText("Match device language")
        snapshot("SCR-0502")
    }

    @Test
    fun fixturesPreserveApiEdgeCases() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val parser = JsonCatalog()
        val catalog =
            parser.catalog(
                context.assets.open("fixtures/catalog.json").bufferedReader().use { it.readText() }
            )
        assertTrue(catalog.shows.any { it.id == catalog.heroShowId })
        assertTrue(catalog.categories.all { it.isCategory })
        assertTrue(catalog.rails.none { it.isCategory })
        val episodes =
            parser.episodes(
                context.assets.open("fixtures/shows/13077.json").bufferedReader().use {
                    it.readText()
                }
            )
        assertEquals(50, episodes.size)
        assertEquals(65, catalog.shows.first { it.id == "13077" }.episodeCount)
        assertTrue(episodes.any { !it.isFree })
        assertNotNull(PlaybackSourceResolver().resolve(episodes.first(), "fr", "en", "2160p"))
    }
}
