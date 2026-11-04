package com.shorts.flowdrama.library

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.layout.ContentScale
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import java.util.Locale

@Composable
internal fun libraryString(id: Int, vararg args: Any): String {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val language = LocalLanguage.current
    val localized = remember(context, config, language) {
        context.createConfigurationContext(Configuration(config).apply { setLocale(Locale.forLanguageTag(language)) })
    }
    return localized.getString(id, *args)
}

internal fun fraction(progress: Progress): Float =
    if (progress.durationMs > 0) (progress.positionMs.toFloat() / progress.durationMs).coerceIn(0f, 1f) else 0f

@Composable
internal fun progressSubtitle(progress: Progress, show: Show, showCompleted: Boolean = false): String {
    val episode = libraryString(com.shorts.flowdrama.feature.library.R.string.library_episode_position,
        progress.episodeNumber, show.episodeCount)
    val remaining = ((progress.durationMs - progress.positionMs).coerceAtLeast(0) + 59999) / 60000
    return if (!showCompleted && !progress.finished && progress.durationMs > 0 && remaining > 0)
        episode + " · " + libraryString(com.shorts.flowdrama.feature.library.R.string.library_minutes_left, remaining)
    else episode
}

@Composable
internal fun LibraryShowRow(
    show: Show,
    subtitle: String,
    onClick: () -> Unit,
    progress: Progress? = null,
    detailProgress: Boolean = false,
    onRemove: (() -> Unit)? = null,
    editing: Boolean = false,
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        val displayedProgress = if (detailProgress && progress?.watched == true) 1f else progress?.let(::fraction) ?: 0f
        Box(Modifier.size(98.dp, 60.dp).clip(RoundedCornerShape(8.dp)).background(artwork(show.id))) {
            LoadingImage(show.posterUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (progress != null) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.Black.copy(alpha = .45f))) {
                    Box(Modifier.fillMaxWidth(displayedProgress).fillMaxHeight().background(Sunset.Gradient))
                }
            }
        }
        Column(Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(0.dp)),
            verticalArrangement = Arrangement.Center) {
            Text(showTitle(show), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 12.sp, color = Sunset.Text3,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detailProgress && progress != null) {
                Spacer(Modifier.height(7.dp))
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(99.dp)).background(Sunset.Surface3)) {
                    Box(Modifier.fillMaxWidth(displayedProgress).fillMaxHeight().background(Sunset.Accent))
                }
            }
        }
        if (onRemove != null) {
            var expanded by remember { mutableStateOf(false) }
            if (editing) {
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                    Icon(FlowIcons.Close, libraryString(com.shorts.flowdrama.feature.library.R.string.library_remove), tint = Sunset.Text3)
                }
            } else Box {
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
                    Icon(FlowIcons.MoreHoriz, libraryString(com.shorts.flowdrama.feature.library.R.string.library_more), tint = Sunset.Text3)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text(libraryString(com.shorts.flowdrama.feature.library.R.string.library_remove)) },
                        onClick = { expanded = false; onRemove() })
                }
            }
        } else Icon(FlowIcons.ChevronRight, null, Modifier.size(24.dp), tint = Sunset.Text3)
    }
}

@Composable
internal fun libraryMetadata(show: Show, catalog: Catalog): String {
    val language = LocalLanguage.current
    val genre = catalog.categories.firstOrNull { it.id in show.tagIds }?.let {
        LocalizedTextResolver().resolve(it.name, language, catalog.defaultLanguage)
    }
    val status = when (show.status.lowercase(Locale.ROOT)) {
        "completed", "complete" -> libraryString(com.shorts.flowdrama.feature.library.R.string.library_complete)
        "airing", "ongoing" -> libraryString(com.shorts.flowdrama.feature.library.R.string.library_airing)
        else -> null
    }
    return listOfNotNull(genre, libraryString(com.shorts.flowdrama.feature.library.R.string.library_episode_count, show.episodeCount), status).joinToString(" · ")
}
