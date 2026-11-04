package com.shorts.flowdrama

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shorts.flowdrama.designsystem.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemStatesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun offlineScreenOffersRetryAndNetworkSettings() {
        var retries = 0
        compose.setContent { FlowTheme("en") { NetworkErrorState { retries++ } } }
        compose.onNodeWithTag("SCR-0603").assertIsDisplayed()
        compose.onNodeWithText("You're offline").assertIsDisplayed()
        compose.onNodeWithText("Open network settings").assertHasClickAction().assertIsDisplayed()
        val connected = com.shorts.flowdrama.network.hasInternetConnection(compose.activity)
        compose.onNodeWithText("Try again").performClick()
        compose.runOnIdle { assertEquals(if (connected) 1 else 0, retries) }
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val file = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots/system-offline.png")
        file.parentFile!!.mkdirs()
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun skeletonKeepsTheRequestedArtworkBounds() {
        compose.setContent { FlowTheme("en") { Skeleton(Modifier.size(98.dp, 64.dp)) } }
        compose.onNodeWithTag("SCR-0601").assertWidthIsEqualTo(98.dp).assertHeightIsEqualTo(64.dp)
    }
}
