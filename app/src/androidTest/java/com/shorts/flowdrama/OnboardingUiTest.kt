package com.shorts.flowdrama

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shorts.flowdrama.data.*
import com.shorts.flowdrama.designsystem.FlowTheme
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.onboarding.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun screenSelectionsLocalizationAndActions() {
        val prefs = MemoryPreferences()
        val reminders = MemoryReminders()
        val vm = OnboardingViewModel(prefs, reminders)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog =
            JsonCatalog()
                .catalog(
                    context.assets.open("fixtures/catalog.json").bufferedReader().use {
                        it.readText()
                    }
                )
        var stage by mutableIntStateOf(0)
        var language by mutableStateOf("en")
        var enableRequests = 0
        var skips = 0
        compose.setContent {
            val preferences by prefs.preferences.collectAsState()
            val windows by reminders.selectedWindows.collectAsState()
            FlowTheme(language) {
                when (stage) {
                    0 -> SplashScreen()
                    1 ->
                        OnboardingScreen1Language(
                            language,
                            false,
                            { code, _ -> language = code },
                            { stage = 2 },
                        )
                    else ->
                        OnboardingFlow(
                            step = stage - 2,
                            selectedMoodIds = preferences.moodIds,
                            selectedTagIds = preferences.tasteIds,
                            categories = catalog.categories,
                            selectedReminderWindows = windows,
                            saveError = false,
                            onToggleMood = { vm.toggleMood(it) },
                            onToggleGenre = vm::toggleGenre,
                            onToggleReminder = vm::toggleReminder,
                            onNext = { stage++ },
                            onSkip = { skips++ },
                            onNotification = { enableRequests++ },
                        )
                }
            }
        }
        screenshot("splash")
        compose.runOnIdle { stage = 1 }
        compose.onNodeWithTag("language:en").assertIsSelected()
        screenshot("language")
        compose.onNodeWithTag("language:ar").performScrollTo().performClick()
        compose.onNodeWithText("Continue").assertExists()
        compose.runOnIdle { assertEquals("en", language) }
        compose.onNodeWithTag("language:ar").assertIsSelected()
        screenshot("language-ar")
        compose.onNodeWithTag("language:en").performScrollTo().performClick()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Every episode open.\nThe story just flows.").assertExists()
        screenshot("value")
        compose.onNodeWithText("Step into the flow").performClick()
        compose.onNodeWithTag("mood:rage").performScrollTo().performClick()
        compose.onNodeWithTag("mood:rage").assertIsOn()
        compose.onNodeWithTag("mood:rage").performClick()
        compose.onNodeWithTag("mood:rage").assertIsOn()
        compose.onNodeWithTag("mood:swoon").performClick()
        compose.onNodeWithTag("mood:swoon").assertIsOn()
        compose.onNodeWithTag("mood:rage").assertIsOff()
        compose.onNodeWithTag("mood:cry").assertIsOff()
        compose.onNodeWithTag("genre:ceo").performScrollTo().assertIsOff().performClick()
        compose.onNodeWithTag("genre:ceo").assertIsOn().performClick()
        compose.onNodeWithTag("genre:ceo").assertIsOff()
        compose.onNodeWithTag("genres:expand").performScrollTo().performClick()
        compose.onNodeWithTag("genre:redemption").performScrollTo().performClick()
        compose.onNodeWithTag("genre:redemption").assertIsOn()
        compose.onNodeWithTag("genre:redemption").performClick()
        compose.onNodeWithTag("genre:redemption").assertIsOff()
        compose.onNodeWithTag("mood:swoon").performScrollTo()
        screenshot("taste")
        compose.onNodeWithText("Skip").assertExists()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithTag("reminder:morning").assertIsOff().performClick()
        compose.onNodeWithTag("reminder:night").performClick()
        compose.runOnIdle {
            assertEquals(
                setOf(ReminderWindow.Morning, ReminderWindow.Night),
                reminders.selectedWindows.value,
            )
        }
        screenshot("notifications")
        compose.onNodeWithTag("reminder:morning").performClick()
        compose.onNodeWithTag("reminder:night").performClick()
        compose.onNodeWithText("Turn on notifications").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, enableRequests) }
        compose.onNodeWithText("Not now").performClick()
        compose.runOnIdle {
            assertEquals(1, skips)
            assertEquals(1, enableRequests)
        }
        compose.onNodeWithTag("reminder:morning").performClick()
        compose.onNodeWithText("Turn on notifications").performClick()
        compose.runOnIdle { assertEquals(2, enableRequests) }
    }

    @Test
    fun reminderChoicesSurviveRepositoryRecreationIncludingEmptySelection() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context
            .getSharedPreferences(SharedPreferencesReminders.FILE_NAME, 0)
            .edit()
            .clear()
            .commit()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val repository = SharedPreferencesReminders(context, scope)
            assertTrue(repository.selectedWindows.value.isEmpty())
            repository.setEnabled(true)
            assertTrue(repository.enabled.value)
            assertTrue(repository.selectedWindows.value.isEmpty())
            repository.toggle(ReminderWindow.Lunch)
            val restored = SharedPreferencesReminders(context, scope)
            assertEquals(
                setOf(ReminderWindow.Lunch),
                withTimeout(5000) {
                    restored.selectedWindows.first { it == setOf(ReminderWindow.Lunch) }
                },
            )
            assertTrue(withTimeout(5000) { restored.enabled.first { it } })
            restored.toggle(ReminderWindow.Lunch)
            assertEquals(
                emptySet<String>(),
                context
                    .getSharedPreferences(SharedPreferencesReminders.FILE_NAME, 0)
                    .getStringSet(SharedPreferencesReminders.WINDOWS_KEY, null),
            )
            val empty = SharedPreferencesReminders(context, scope)
            assertTrue(withTimeout(5000) { empty.selectedWindows.first { it.isEmpty() } }.isEmpty())
            assertTrue(withTimeout(5000) { empty.enabled.first { it } })
            assertTrue(
                context
                    .getSharedPreferences(SharedPreferencesReminders.FILE_NAME, 0)
                    .getBoolean(SharedPreferencesReminders.ENABLED_KEY, true)
            )
        } finally {
            scope.cancel()
        }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(300)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file =
            File(
                instrumentation.targetContext.getExternalFilesDir(null),
                "onboarding-review/$name.png",
            )
        file.parentFile!!.mkdirs()
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            file.outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }
}

private class MemoryPreferences : PreferencesRepository {
    override val preferences = MutableStateFlow(Preferences())

    override suspend fun update(transform: (Preferences) -> Preferences) {
        preferences.value = transform(preferences.value)
    }
}

private class MemoryReminders : ReminderPreferencesRepository {
    override val selectedWindows = MutableStateFlow<Set<ReminderWindow>>(emptySet())
    override val enabled = MutableStateFlow(false)

    override suspend fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }

    override suspend fun toggle(window: ReminderWindow) {
        selectedWindows.value =
            if (window in selectedWindows.value) selectedWindows.value - window
            else selectedWindows.value + window
    }
}
