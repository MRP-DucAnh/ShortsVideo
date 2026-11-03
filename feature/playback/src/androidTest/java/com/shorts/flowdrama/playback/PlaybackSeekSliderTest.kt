package com.shorts.flowdrama.playback

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shorts.flowdrama.designsystem.FlowTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackSeekSliderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun keepVisible() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            compose.activity.setShowWhenLocked(true)
            compose.activity.setTurnScreenOn(true)
        }
    }

    @Test fun thumbIsVerticallyCenteredAlongTrack() {
        val progress = mutableFloatStateOf(0f)
        compose.setContent {
            FlowTheme("en") {
                Column {
                    PlaybackSeekSlider(progress.floatValue, { progress.floatValue = it }, {}, true)
                    PlaybackTimeLabels(1000, 60000)
                }
            }
        }
        for (value in listOf(0f, .5f, 1f)) {
            compose.runOnIdle { progress.floatValue = value }
            val thumb = compose.onNodeWithTag("playback:thumb", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val track = compose.onNodeWithTag("playback:track", useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertEquals((track.top.value + track.bottom.value) / 2, (thumb.top.value + thumb.bottom.value) / 2, .5f)
            val expectedX = track.left.value + (track.right.value - track.left.value) * value
            assertEquals(expectedX, (thumb.left.value + thumb.right.value) / 2, 1f)
            val elapsed = compose.onNodeWithTag("playback:elapsed", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val duration = compose.onNodeWithTag("playback:duration", useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertEquals(track.left.value, elapsed.left.value, 1f)
            assertEquals(track.right.value, duration.right.value, 1f)
        }
    }
}
