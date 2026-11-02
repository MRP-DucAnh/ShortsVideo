package com.shorts.flowdrama

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run after the offline onboarding journey, with network restored. */
@RunWith(AndroidJUnit4::class)
class PlaybackJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitText(text: String) {
        compose.waitUntil(20000) {
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

    private fun dismissSheet() {
        val dismiss =
            SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.Dismiss)
        compose.onAllNodes(dismiss).onLast().performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.Dismiss
        ) {
            it()
        }
        compose.waitUntil(10000) { compose.onAllNodes(dismiss).fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
    }

    @Test
    fun playerSheetsLocksAndPersistentResume() {
        waitText("Home")
        val container = (compose.activity.application as FlowDramaApplication).container
        val showId = "12692"
        kotlinx.coroutines.runBlocking {
            container.preferences.update {
                it.copy(speed = 1f, quality = "Auto", audioLanguage = "en")
            }
            if (showId !in container.library.savedIds.value) container.library.toggleSaved(showId)
            container.library.removeHistory(showId)
        }
        compose.onNodeWithText("Library").performClick()
        compose.onNodeWithText("Saved").performClick()
        waitText("Saved")
        val catalog = kotlinx.coroutines.runBlocking { container.catalog.catalog() }
        val show = catalog.shows.first { it.id == showId }
        compose.onNodeWithText(show.title.getValue("en")).performClick()
        waitText("Play Ep 1")
        snapshot("SCR-0301")
        compose.onNodeWithText("Play Ep 1").performClick()
        compose.waitUntil(20000) {
            runCatching {
                    compose
                        .onAllNodesWithContentDescription("Playback options")
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                .getOrDefault(false)
        }
        snapshot("SCR-0302")
        compose.onNodeWithContentDescription("Episodes").performClick()
        waitText("Episodes")
        snapshot("SCR-0303")
        val episodes = kotlinx.coroutines.runBlocking { container.catalog.episodes(showId) }
        val locked = episodes.first { !it.isFree }
        compose.onNodeWithText(locked.number.toString()).performClick()
        waitText("Episode locked")
        snapshot("locked-dialog")
        compose.onNodeWithText("OK").performClick()
        dismissSheet()
        compose.onNodeWithContentDescription("Playback options").performClick()
        waitText("Quality")
        snapshot("SCR-0304")
        compose.onAllNodesWithText("1.5×").onLast().performClick()
        dismissSheet()
        compose.waitUntil(60000) {
            container.library.history.value.any {
                it.showId == showId && it.positionMs > 2000 && it.durationMs > 0
            }
        }
        compose.onNodeWithText("Pause").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        waitText("Resume")
        val checkpoint = container.library.history.value.first { it.showId == showId }
        assertTrue(checkpoint.positionMs > 2000)
        compose.activityRule.scenario.recreate()
        waitText("Resume")
        val restored = container.library.history.value.first { it.showId == showId }
        assertEquals(checkpoint.episodeNumber, restored.episodeNumber)
        assertTrue(restored.positionMs >= checkpoint.positionMs - 1000)
    }
}
