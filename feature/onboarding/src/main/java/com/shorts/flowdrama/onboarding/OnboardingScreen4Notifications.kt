package com.shorts.flowdrama.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.ReminderWindow
import com.shorts.flowdrama.feature.onboarding.R
import com.shorts.flowdrama.onboarding.components.*
import java.util.Locale

@Composable
internal fun OnboardingScreen4Notifications(
    selected: Set<ReminderWindow>,
    saveError: Boolean,
    onToggle: (ReminderWindow) -> Unit,
    onEnable: () -> Unit,
    onSkip: () -> Unit,
) {
    OnboardingPage(
        step = 4,
        tag = "onboarding:notifications",
        action = onboardingString(R.string.ob_turn_on_notifications),
        onAction = onEnable,
        secondaryAction = onboardingString(R.string.ob_not_now),
        onSecondaryAction = onSkip,
        saveError = saveError,
    ) {
        OnboardingHeadline(
            onboardingString(R.string.ob_notifications_title),
            onboardingString(R.string.ob_notifications_description),
        )
        Spacer(Modifier.height(20.dp))
        ReminderOptions(selected, onToggle)

    }
}

@Composable
private fun ReminderOptions(selected: Set<ReminderWindow>, onToggle: (ReminderWindow) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        ReminderWindow.entries.forEach { window ->
            ReminderOption(window, window in selected) { onToggle(window) }
        }
    }
}

@Composable
private fun ReminderOption(window: ReminderWindow, selected: Boolean, onClick: () -> Unit) {
    val title =
        when (window) {
            ReminderWindow.Morning -> R.string.ob_morning
            ReminderWindow.Lunch -> R.string.ob_lunch
            ReminderWindow.Night -> R.string.ob_night
        }
    val subtitle =
        when (window) {
            ReminderWindow.Morning -> R.string.ob_morning_description
            ReminderWindow.Lunch -> R.string.ob_lunch_description
            ReminderWindow.Night -> R.string.ob_night_description
        }
    val emoji =
        when (window) {
            ReminderWindow.Morning -> "☕"
            ReminderWindow.Lunch -> "🌤"
            ReminderWindow.Night -> "🌙"
        }
    val time =
        String.format(
            Locale.forLanguageTag(LocalLanguage.current),
            "%02d:%02d",
            window.hour,
            window.minute,
        )
    val foreground = if (selected) Sunset.OnAccent else Sunset.Text
    SelectableOption(selected, onClick, Modifier.fillMaxWidth().testTag("reminder:${window.id}")) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(emoji, fontSize = 20.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    onboardingString(R.string.ob_reminder_heading, onboardingString(title), time),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = foreground,
                )
                Text(
                    onboardingString(subtitle),
                    Modifier.padding(top = 3.dp),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = if (selected) foreground.copy(alpha = .75f) else Sunset.Text3,
                )
            }
            if (selected) {
                Text("✓", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
