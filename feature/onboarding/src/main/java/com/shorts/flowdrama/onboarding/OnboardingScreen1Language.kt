package com.shorts.flowdrama.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.feature.onboarding.R
import com.shorts.flowdrama.onboarding.components.*

/** Step 1: keep the choice local until Continue commits it. */
@Composable
fun OnboardingScreen1Language(
    language: String,
    followDevice: Boolean,
    onSelect: (String, Boolean) -> Unit,
    onContinue: () -> Unit = {},
) {
    var pendingLanguage by rememberSaveable { mutableStateOf(language) }
    var pendingFollowDevice by rememberSaveable { mutableStateOf(followDevice) }
    OnboardingPage(
        step = 1,
        tag = "onboarding:language",
        action = onboardingString(R.string.ob_continue),
        onAction = {
            onSelect(pendingLanguage, pendingFollowDevice)
            onContinue()
        },
    ) {
        LanguageIntroduction()
        Spacer(Modifier.height(16.dp))
        LanguageChoices(pendingLanguage, pendingFollowDevice, onboarding = true) { code, device ->
            pendingLanguage = code
            pendingFollowDevice = device
        }
    }
}

/** Settings applies each language choice immediately. */
@Composable
fun SettingsLanguageScreen(
    language: String,
    followDevice: Boolean,
    onSelect: (String, Boolean) -> Unit,
    onBack: () -> Unit,
) = SettingsLanguageContent(language, followDevice, onSelect, onBack)

@Composable
private fun LanguageIntroduction() {
    Text(
        onboardingString(R.string.ob_language_title),
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Sunset.Text,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        onboardingString(R.string.ob_language_description),
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = Sunset.Text2,
    )
}

@Composable
private fun LanguageChoices(
    language: String,
    followDevice: Boolean,
    onboarding: Boolean,
    onSelect: (String, Boolean) -> Unit,
) {
    val initialLanguage = rememberSaveable { language }
    val choices = remember(initialLanguage, onboarding) {
        if (onboarding) launchLanguages.sortedBy { if (it.first == initialLanguage) 0 else 1 }
        else launchLanguages
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        choices.forEach { (code, name) ->
            LanguageOption(
                name,
                code,
                language == code && (onboarding || !followDevice),
                onboarding,
            ) {
                onSelect(code, false)
            }
        }
    }
}

@Composable
private fun LanguageOption(
    name: String,
    code: String,
    selected: Boolean,
    onboarding: Boolean,
    onClick: () -> Unit,
) {
    val background =
        when {
            selected -> Sunset.Accent
            onboarding -> Sunset.Surface.copy(alpha = .72f)
            else -> MaterialTheme.colorScheme.surface
        }
    val foreground =
        when {
            selected -> Sunset.OnAccent
            onboarding -> Sunset.Text2
            else -> MaterialTheme.colorScheme.onSurface
        }
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("language:$code")
            .clip(CircleShape)
            .background(background)
            .border(1.dp, if (selected) Sunset.Accent else Sunset.Border, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            Modifier.weight(1f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = foreground,
        )
        if (selected)
            Text("✓", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsLanguageContent(
    language: String,
    followDevice: Boolean,
    onSelect: (String, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(FlowIcons.ArrowBack, onboardingString(R.string.ob_back))
            }
            Text(
                onboardingString(R.string.ob_settings_language_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(
                onboardingString(R.string.ob_settings_language_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))
            LanguageOption(
                onboardingString(R.string.ob_match_device),
                "device",
                followDevice,
                false,
            ) {
                onSelect(language, true)
            }
            Text(
                onboardingString(R.string.ob_choose_manually),
                Modifier.padding(vertical = 16.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LanguageChoices(language, followDevice, false, onSelect)
        }
    }
}
