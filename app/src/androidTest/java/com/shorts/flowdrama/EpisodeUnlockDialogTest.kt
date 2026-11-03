package com.shorts.flowdrama

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shorts.flowdrama.designsystem.FlowTheme
import com.shorts.flowdrama.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EpisodeUnlockDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Before fun keepVisible() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            compose.activity.setShowWhenLocked(true)
            compose.activity.setTurnScreenOn(true)
        }
    }
    @Test fun rewardPromptIsLocalizedAndCanBeCancelledWithoutUnlocking() {
        val prefs = object : PreferencesRepository {
            override val preferences = MutableStateFlow(Preferences())
            override suspend fun update(transform: (Preferences) -> Preferences) {
                preferences.value = transform(preferences.value)
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val unlock = EpisodeUnlockCoordinator(prefs, scope)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as FlowDramaApplication
        unlock.request("12692", 6)
        try {
            compose.setContent {
                val state by unlock.state.collectAsState()
                FlowTheme("fr") {
                    EpisodeUnlockDialog(application.container.ads, unlock, state, compose.activity, "Test drama")
                }
            }
            compose.onNodeWithText("Débloquer l’épisode 6").assertExists()
            compose.onNodeWithText("Pas maintenant").performClick()
            compose.onNodeWithTag("ads:episode-unlock").assertDoesNotExist()
            compose.runOnIdle { assertTrue(prefs.preferences.value.unlockedEpisodeIds.isEmpty()) }
        } finally { scope.cancel() }
    }
}
