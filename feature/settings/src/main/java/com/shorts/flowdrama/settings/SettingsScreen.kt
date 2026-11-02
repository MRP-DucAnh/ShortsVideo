package com.shorts.flowdrama.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*

@Composable
fun SettingsScreen(
    preferences: Preferences,
    version: String,
    onLanguage: () -> Unit,
    onUpdate: ((Preferences) -> Preferences) -> Unit,
    onPolicy: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Heading("Settings")
        GroupLabel("Language")
        SettingsRow(
            "App language",
            launchLanguages.find { it.first == preferences.language }?.second ?: "English",
            onLanguage,
        )
        GroupLabel("Appearance")
        SettingsRow("Dark", if (!preferences.lightTheme) "✓" else "") {
            onUpdate { it.copy(lightTheme = false) }
        }
        SettingsRow("Light", if (preferences.lightTheme) "✓" else "") {
            onUpdate { it.copy(lightTheme = true) }
        }
        GroupLabel("Playback options")
        Row(
            Modifier.padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tr("Preview on Wi-Fi only"), Modifier.weight(1f), fontSize = 14.sp)
            Switch(
                checked = preferences.wifiPreviewOnly,
                onCheckedChange = { checked -> onUpdate { it.copy(wifiPreviewOnly = checked) } },
            )
        }
        GroupLabel("Policies")
        listOf("Terms of Use", "Privacy Policy", "Open-source licenses").forEach { title ->
            SettingsRow(title, "›") { onPolicy(title) }
        }
        Text(
            "FlowDrama v$version",
            Modifier.fillMaxWidth().padding(24.dp),
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Sunset.Text3,
        )
    }
}

@Composable
private fun GroupLabel(title: String) {
    Text(
        tr(title).uppercase(),
        Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Sunset.Text3,
    )
}

@Composable
private fun SettingsRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tr(title), Modifier.weight(1f), fontSize = 14.sp)
        Text(value, color = if (value == "✓") Sunset.Accent else Sunset.Text3, fontSize = 13.sp)
    }
}
