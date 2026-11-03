package com.shorts.flowdrama.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*

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
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(250.dp)) {
            AsyncImage(
                show.posterUrl,
                null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Sunset.Scrim))
            Heading("", onBack) {
                Glyph(
                    if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    "Save",
                    onSave,
                    Sunset.Text,
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    showTitle(show),
                    fontSize = 26.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Sunset.Text,
                )
                Text(
                    show.tagIds.take(3).joinToString("  ·  "),
                    fontSize = 12.sp,
                    color = Sunset.Text2,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column {
                Text(
                    compactCount(show.viewCount, LocalLanguage.current),
                    fontWeight = FontWeight.Bold,
                )
                Text("Views", fontSize = 11.sp, color = Sunset.Text3)
            }
            Column {
                Text((episodes?.size ?: show.episodeCount).toString(), fontWeight = FontWeight.Bold)
                Text(tr("Episodes"), fontSize = 11.sp, color = Sunset.Text3)
            }
            Column {
                Text(show.status, fontWeight = FontWeight.Bold)
                Text("Status", fontSize = 11.sp, color = Sunset.Text3)
            }
        }
        Text(
            LocalizedTextResolver().resolve(show.synopsis, LocalLanguage.current),
            Modifier.padding(horizontal = 16.dp).clickable { expanded = !expanded },
            maxLines = if (expanded) 8 else 3,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(
                if (progress == null) "Play Ep 1" else "Resume",
                { onPlay(progress?.episodeNumber ?: -1) },
                Modifier.weight(1f),
            )
            Glyph(if (saved) Icons.Outlined.Bookmark else Icons.Outlined.Add, "Save", onSave)
        }
        SectionTitle("Episodes")
        when {
            error -> TextButton(onClick = onRetry) { Text(tr("Try again")) }
            episodes == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
            else ->
                EpisodeGrid(episodes, progress?.episodeNumber, emptySet(), Modifier.weight(1f)) {
                    if (it.isFree) onPlay(it.number) else onLocked(it)
                }
        }
    }
}
