package com.shorts.flowdrama.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.shorts.flowdrama.designsystem.FlowTheme
import com.shorts.flowdrama.domain.*

@Preview(name = "Splash", widthDp = 390, heightDp = 844, showSystemUi = true)
@Composable
private fun SplashPreview() = FlowTheme("en") { SplashScreen() }

@Preview(name = "1 · Language", widthDp = 390, heightDp = 844, showSystemUi = true)
@Composable
private fun LanguagePreview() =
    FlowTheme("en") {
        OnboardingScreen1Language(
            language = "en",
            followDevice = false,
            onSelect = { _, _ -> },
        )
    }

@Preview(name = "2 · Value proposition", widthDp = 390, heightDp = 844, showSystemUi = true)
@Composable
private fun ValuePreview() = FlowTheme("en") { OnboardingScreen2ValueProposition(onContinue = {}) }

@Preview(name = "3 · Taste picker", widthDp = 390, heightDp = 844, showSystemUi = true)
@Composable
private fun TastePreview() =
    FlowTheme("en") {
        OnboardingScreen3TastePicker(
            moodIds = setOf("rage"),
            tagIds = setOf("revenge", "urban"),
            categories = previewCategories,
            saveError = false,
            onMood = {},
            onGenre = {},
            onContinue = {},
            onSkip = {},
        )
    }

@Preview(name = "4 · Notifications", widthDp = 390, heightDp = 844, showSystemUi = true)
@Composable
private fun RemindersPreview() =
    FlowTheme("en") {
        OnboardingScreen4Notifications(
            selected = setOf(ReminderWindow.Morning),
            saveError = false,
            onToggle = {},
            onEnable = {},
            onSkip = {},
        )
    }

private val previewCategories =
    listOf(
            "fantasy",
            "ceo",
            "sweet-romance",
            "comedy",
            "plot-twist",
            "urban",
            "contract-marriage",
            "marriage-first-love-later",
            "angsty-romance",
            "reunion",
            "love",
            "comeback",
            "revenge",
            "underdog-triumph",
            "redemption",
        )
        .mapIndexed { index, id -> Tag(id, mapOf("en" to id), true, index) }
