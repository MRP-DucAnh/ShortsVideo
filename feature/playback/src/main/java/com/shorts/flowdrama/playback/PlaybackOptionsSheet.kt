package com.shorts.flowdrama.playback

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.feature.playback.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaybackOptionsSheet(
    episode: Episode?,
    source: PlaybackSource?,
    preferences: Preferences,
    onDismiss: () -> Unit,
    onPreferences: ((Preferences) -> Preferences) -> Unit,
    onAudioLanguage: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            Text(playbackString(R.string.player_options), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
            Text(playbackString(R.string.player_speed), fontWeight = FontWeight.Bold)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                    TagChip("${speed}×", preferences.speed == speed) {
                        onPreferences { it.copy(speed = speed) }
                    }
                }
            }
            Text(playbackString(R.string.player_quality), Modifier.padding(top = 16.dp), fontWeight = FontWeight.Bold)
            val qualities =
                listOf("Auto") +
                    (episode?.sources?.get(source?.language)?.keys?.toList().orEmpty())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                qualities.forEach { quality ->
                    TagChip(if (quality == "Auto") playbackString(R.string.player_auto) else quality, preferences.quality == quality) {
                        onPreferences { it.copy(quality = quality) }
                    }
                }
            }
            Text(
                playbackString(R.string.player_audio),
                Modifier.padding(top = 16.dp),
                fontWeight = FontWeight.Bold,
            )
            episode?.sources?.keys?.forEach { code ->
                TextButton(
                    onClick = {
                        onAudioLanguage(code)
                    }
                ) {
                    Text(
                        (launchLanguages.find { it.first == code }?.second ?: code) +
                            if (source?.language == code) "  ✓" else ""
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
