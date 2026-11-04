package com.shorts.flowdrama

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.Preferences
import com.shorts.flowdrama.settings.SettingsScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalizationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun sharedStringsAndFormattedActionsUseEverySelectedAppLanguage() {
        val expected = mapOf("en" to "Settings", "ar" to "الإعدادات", "de" to "Einstellungen",
            "es" to "Ajustes", "fr" to "Paramètres", "hi" to "सेटिंग", "id" to "Setelan",
            "pt" to "Configurações", "ta" to "அமைப்புகள்", "tr" to "Ayarlar")
        for ((language, settings) in expected) {
            assertEquals(language, settings, localizedUiText(compose.activity, language, "Settings"))
            val action = localizedUiText(compose.activity, language, "Watch all %1\$d episodes", 12)
            assertFalse("Unresolved placeholder for $language: $action", action.contains("%1"))
            assertNotEquals("Watch all %1\$d episodes", action)
            if (language != "en") {
                assertNotEquals(language, "You're offline", localizedUiText(compose.activity, language, "You're offline"))
                assertNotEquals(language, "Switch back to HD", localizedUiText(compose.activity, language, "Switch back to HD"))
            }
        }
    }

    @Test fun settingsRendersSelectedLanguageRatherThanEnglishFallback() {
        compose.setContent {
            FlowTheme("de") {
                SettingsScreen(Preferences(language = "de"), "test", {}, {}, {})
            }
        }
        compose.onNodeWithText("Einstellungen").assertExists()
        compose.onNodeWithText("App-Sprache").assertExists()
        compose.onNodeWithText("Settings").assertDoesNotExist()
        compose.onNodeWithText("Preview on Wi-Fi only").assertDoesNotExist()
    }
}
