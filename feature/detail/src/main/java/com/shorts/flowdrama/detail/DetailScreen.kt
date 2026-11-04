package com.shorts.flowdrama.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*

private const val PreviewEpisodeCount = 10

@Composable
fun DetailScreen(
    show: Show,
    episodes: List<Episode>?,
    saved: Boolean,
    progress: Progress?,
    error: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onPlay: (Int) -> Unit,
    onLocked: (Episode) -> Unit,
    watched: Set<Int> = emptySet(),
    tags: List<Tag> = emptyList(),
) {
    val online by com.shorts.flowdrama.network.rememberOnline()
    LaunchedEffect(online) { if (online && error) onRetry() }
    var rangesExpanded by rememberSaveable(show.id) { mutableStateOf(false) }
    var selectedRange by rememberSaveable(show.id) { mutableIntStateOf(0) }
    val episodeRanges = remember(episodes) { episodes.orEmpty().chunked(20) }
    val selectedEpisodes = episodeRanges.getOrNull(selectedRange) ?: episodeRanges.firstOrNull().orEmpty()
    val currentNumber = progress?.episodeNumber
        ?.takeIf { number -> episodes == null || episodes.any { it.number == number } }
        ?: episodes?.firstOrNull()?.number ?: 1
    val onEpisode: (Episode) -> Unit = { episode ->
        if (episode.isFree) onPlay(episode.number) else onLocked(episode)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DetailHero(show, tags, onBack)
        DetailStats(episodes?.size ?: show.episodeCount, show.viewCount)
        DetailSynopsis(show)
        DetailActions(
            currentNumber, saved, !episodes.isNullOrEmpty(), onSave,
            onContinue = { episodes?.find { it.number == currentNumber }?.let(onEpisode) },
        )
        DetailEpisodesHeader(
            episodes?.lastOrNull()?.number ?: show.episodeCount,
            !episodes.isNullOrEmpty(),
            expanded = rangesExpanded,
            onExpand = { rangesExpanded = !rangesExpanded },
        )
        if (rangesExpanded && episodeRanges.isNotEmpty()) {
            DetailEpisodeRanges(episodeRanges, selectedRange) { selectedRange = it }
        }
        when {
            !online && episodes == null -> Box(Modifier.fillMaxWidth().height(380.dp)) { NetworkErrorState(onRetry) }
            error -> Box(Modifier.fillMaxWidth().height(320.dp)) {
                BlankState("Unable to load episodes", "Please try again.", "Try again", onClick = onRetry)
            }
            episodes == null -> EpisodeGridSkeleton()
            episodes.isNotEmpty() -> {
                val preview = if (rangesExpanded) selectedEpisodes else selectedEpisodes.take(PreviewEpisodeCount)
                val rowCount = (preview.size + 4) / 5
                EpisodeGrid(preview, currentNumber, watched,
                    Modifier.fillMaxWidth().height((32 + rowCount * 48 + (rowCount - 1) * 9).dp)
                        .testTag("detail:episode-preview"), onEpisode)
            }
        }
    }
}

@Composable
private fun DetailHero(show: Show, tags: List<Tag>, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(320.dp).testTag("detail:hero")) {
        LoadingImage(show.posterUrl, showTitle(show), Modifier.fillMaxSize(), ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(96.dp).background(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = .55f), Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(170.dp).align(Alignment.BottomCenter).background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .88f)))))
        Glyph(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack, Color.White)
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp)) {
            Text(showTitle(show), color = Color.White, fontSize = 22.sp, lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val genres = show.tagIds.mapNotNull { id -> tags.find { it.id == id && it.isCategory } }.take(3)
            if (genres.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    genres.forEach { genre -> DetailGenreTag(genre) }
                }
            }
        }
    }
}

@Composable
private fun DetailGenreTag(tag: Tag) {
    Text(LocalizedTextResolver().resolve(tag.name, LocalLanguage.current),
        modifier = Modifier.clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = .15f))
            .border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        color = Color.White.copy(alpha = .85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun DetailStats(episodeCount: Int, views: Long) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        DetailStat(episodeCount.toString(), tr("Episodes"))
        DetailStat(compactCount(views, LocalLanguage.current), tr("Views"))
    }
}

@Composable
private fun DetailStat(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailSynopsis(show: Show) {
    var expanded by rememberSaveable(show.id) { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(LocalizedTextResolver().resolve(show.synopsis, LocalLanguage.current),
            maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(tr(if (expanded) "Less" else "More"),
            Modifier.heightIn(min = 48.dp).clickable { expanded = !expanded }.wrapContentHeight(),
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailActions(number: Int, saved: Boolean, enabled: Boolean, onSave: () -> Unit, onContinue: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(999.dp))
            .background(Sunset.Gradient).clickable(enabled = enabled, onClick = onContinue)
            .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PlayArrow, null, tint = Sunset.OnAccent, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("Continue Ep. %1\$d", number), color = Sunset.OnAccent,
                fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onSave, modifier = Modifier.size(54.dp, 48.dp)
            .clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .4f), RoundedCornerShape(999.dp))) {
            Icon(if (saved) Icons.Outlined.Bookmark else Icons.Outlined.Add,
                tr(if (saved) "Remove" else "Save"), tint = if (saved) Sunset.Accent else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun DetailEpisodesHeader(lastNumber: Int, enabled: Boolean, expanded: Boolean, onExpand: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(tr("Episodes"), Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = onExpand, enabled = enabled, modifier = Modifier.testTag("detail:episode-picker")) {
            Text("1–$lastNumber", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Expands in the detail page; changing a range replaces the grid directly below it. */
@Composable
private fun DetailEpisodeRanges(ranges: List<List<Episode>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp).testTag("detail:episode-ranges"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ranges.forEachIndexed { index, episodes ->
            TagChip("${episodes.first().number}–${episodes.last().number}", selected == index) { onSelect(index) }
        }
    }
}
