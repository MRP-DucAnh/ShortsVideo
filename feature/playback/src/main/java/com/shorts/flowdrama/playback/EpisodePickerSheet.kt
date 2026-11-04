package com.shorts.flowdrama.playback

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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

@Composable
internal fun EpisodePickerSheet(
    episodes: List<Episode>,
    current: Int?,
    watched: Set<Int>,
    onDismiss: () -> Unit,
    onEpisode: (Episode) -> Unit,
) = EpisodePicker(episodes, current, watched, onDismiss, onEpisode)
