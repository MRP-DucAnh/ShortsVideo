package com.shorts.flowdrama.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import java.util.Locale

private val GroupShape = RoundedCornerShape(12.dp)

@Composable
fun SettingsScreen(
    preferences: Preferences,
    version: String,
    onLanguage: () -> Unit,
    onUpdate: ((Preferences) -> Preferences) -> Unit,
    onPolicy: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(tr("Settings"), Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
            fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
        SettingsGroup("Language") {
            SettingsRow("App language", value = launchLanguages.find { it.first == preferences.language }?.second ?: "English",
                showChevron = true, onClick = onLanguage)
        }
        SettingsGroup("Appearance", Modifier.selectableGroup()) {
            SettingsRow("Dark", selected = !preferences.lightTheme) { onUpdate { it.copy(lightTheme = false) } }
            SettingsDivider()
            SettingsRow("Light", selected = preferences.lightTheme) { onUpdate { it.copy(lightTheme = true) } }
        }
        SettingsGroup("Playback options") {
            SettingsPreviewToggle(preferences.wifiPreviewOnly) { checked -> onUpdate { it.copy(wifiPreviewOnly = checked) } }
        }
        SettingsGroup("Policies") {
            val policies = listOf("Terms of Use", "Privacy Policy", "Open-source licenses")
            policies.forEachIndexed { index, title ->
                if (index > 0) SettingsDivider()
                SettingsRow(title, showChevron = true) { onPolicy(title) }
            }
        }
        Text("FlowDrama v$version", Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 30.dp),
            fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f))
    }
}

/** One rounded card per group, matching SCR-0501 instead of separate cards per row. */
@Composable
private fun SettingsGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Text(tr(title).uppercase(Locale.forLanguageTag(LocalLanguage.current)),
        Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 7.dp),
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.sp,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(GroupShape)
        .background(MaterialTheme.colorScheme.surface).then(modifier), content = content)
}

@Composable
private fun SettingsRow(
    title: String,
    value: String = "",
    showChevron: Boolean = false,
    selected: Boolean? = null,
    onClick: () -> Unit,
) {
    val interaction = if (selected != null) Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick)
        else Modifier.clickable(onClick = onClick)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).then(interaction)
        .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(tr(title), Modifier.weight(1f), fontSize = 14.sp, lineHeight = 18.sp)
        if (value.isNotEmpty()) Text(value, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(max = 150.dp))
        if (selected == true) Icon(FlowIcons.Check, null, Modifier.size(18.dp), tint = Sunset.Accent)
        if (showChevron) Icon(
            FlowIcons.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = .3f))
}

/** The compact 44×26 visual sits inside a row with a 48dp minimum touch target. */
@Composable
private fun SettingsPreviewToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
        .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(tr("Preview on Wi-Fi only"), Modifier.weight(1f), fontSize = 14.sp, lineHeight = 18.sp)
        Box(Modifier.size(44.dp, 26.dp).clip(RoundedCornerShape(999.dp))
            .background(if (checked) Sunset.Accent else MaterialTheme.colorScheme.surfaceVariant).padding(3.dp)) {
            Box(Modifier.size(20.dp).align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .background(if (checked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    RoundedCornerShape(999.dp)))
        }
    }
}
