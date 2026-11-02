package com.shorts.flowdrama.onboarding

import androidx.compose.runtime.Composable
import com.shorts.flowdrama.domain.*

/** Routes only. Open the named screen file to read its header, content and footer. */
@Composable
fun OnboardingFlow(
    step: Int,
    selectedMoodIds: Set<String>,
    selectedTagIds: Set<String>,
    categories: List<Tag>,
    selectedReminderWindows: Set<ReminderWindow>,
    saveError: Boolean,
    onToggleMood: (TasteMood) -> Unit,
    onToggleGenre: (String) -> Unit,
    onToggleReminder: (ReminderWindow) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onNotification: () -> Unit,
) {
    when (step) {
        0 -> OnboardingScreen2ValueProposition(onContinue = onNext)
        1 ->
            OnboardingScreen3TastePicker(
                moodIds = selectedMoodIds,
                tagIds = selectedTagIds,
                categories = categories,
                saveError = saveError,
                onMood = onToggleMood,
                onGenre = onToggleGenre,
                onContinue = onNext,
                onSkip = onSkip,
            )
        2 ->
            OnboardingScreen4Notifications(
                selected = selectedReminderWindows,
                saveError = saveError,
                onToggle = onToggleReminder,
                onEnable = onNotification,
                onSkip = onSkip,
            )
        else -> error("Unknown onboarding step: $step")
    }
}
