package com.shorts.flowdrama.playback

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.Episode
import com.shorts.flowdrama.feature.playback.R
import java.util.Locale

@Composable
internal fun playbackString(id: Int, vararg args: Any): String {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val language = LocalLanguage.current
    val localized = remember(context, config, language) {
        context.createConfigurationContext(Configuration(config).apply {
            setLocale(Locale.forLanguageTag(language))
        })
    }
    return localized.getString(id, *args)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EpisodePickerSheet(
    episodes: List<Episode>,
    current: Int?,
    watched: Set<Int>,
    onDismiss: () -> Unit,
    onEpisode: (Episode) -> Unit,
) {
    val pages = episodes.chunked(20)
    var page by rememberSaveable(episodes.size, current) {
        mutableIntStateOf((episodes.indexOfFirst { it.number == current }.coerceAtLeast(0) / 20))
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Sunset.Surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(playbackString(R.string.player_episodes), fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, color = Sunset.Text)
                Text(playbackString(R.string.player_episode_summary, episodes.size) + " · " +
                        playbackString(R.string.player_watched_count, watched.size),
                    fontSize = 13.sp, color = Sunset.Text3)
            }
            Row(Modifier.horizontalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { index, group ->
                    TagChip("${group.first().number}–${group.last().number}", page == index) { page = index }
                }
            }
            EpisodeGrid(pages.getOrNull(page).orEmpty(), current, watched,
                Modifier.fillMaxWidth().heightIn(max = 340.dp), onEpisode)
        }
    }
}
