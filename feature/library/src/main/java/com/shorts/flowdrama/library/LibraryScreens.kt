package com.shorts.flowdrama.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.ads.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*

@Composable
fun LibraryScreen(
    catalog: Catalog,
    history: List<Progress>,
    saved: Set<String>,
    ads: AdsController,
    onHistory: () -> Unit,
    onSaved: () -> Unit,
    onShow: (String) -> Unit,
) {
    LazyColumn {
        item {
            Heading("Library")
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibraryCard(
                    "Watch history",
                    history.size,
                    Icons.Outlined.History,
                    Modifier.weight(1f),
                    onHistory,
                )
                LibraryCard(
                    "Saved",
                    saved.size,
                    Icons.Outlined.BookmarkBorder,
                    Modifier.weight(1f),
                    onSaved,
                )
            }
            SectionTitle("Recently watched", "See all", onHistory)
        }
        items(history.take(6), key = { it.showId }, contentType = { "recent" }) { progress ->
            catalog.shows
                .find { it.id == progress.showId }
                ?.let {
                    ShowRow(
                        it,
                        "Ep. ${progress.episodeNumber} · ${if(progress.finished) tr("Finished") else tr("Watching")}",
                        { onShow(it.id) },
                    )
                }
        }
        if (history.isEmpty())
            item {
                Text(
                    tr("Start a drama and pick up where you left off."),
                    Modifier.padding(16.dp),
                    color = Sunset.Text3,
                )
            }
        item { LibraryBanner(ads) }
    }
}

@Composable
private fun LibraryCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Icon(icon, null, tint = Sunset.Accent)
        Text(
            count.toString(),
            Modifier.padding(top = 20.dp),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(tr(title), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HistoryScreen(
    catalog: Catalog,
    history: List<Progress>,
    onBack: () -> Unit,
    onShow: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onBrowse: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Heading("Watch history", onBack) {
            Glyph(Icons.Outlined.DeleteOutline, "Clear history", { confirm = true })
        }
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TagChip("Watching", !filter) { filter = false }
            TagChip("Finished", filter) { filter = true }
        }
        val filtered = history.filter { it.finished == filter }
        if (filtered.isEmpty())
            BlankState(
                "No watch history yet",
                "Start a drama and pick up where you left off.",
                "Browse",
                Icons.Outlined.History,
                onBrowse,
            )
        else
            LazyColumn {
                items(filtered, key = { it.showId }, contentType = { "history" }) { progress ->
                    catalog.shows
                        .find { it.id == progress.showId }
                        ?.let { show ->
                            Column {
                                ShowRow(
                                    show,
                                    "Ep. ${progress.episodeNumber} / ${show.episodeCount}",
                                    { onShow(show.id) },
                                ) {
                                    Glyph(Icons.Outlined.Close, "Remove", { onRemove(show.id) })
                                }
                                LinearProgressIndicator(
                                    progress = {
                                        if (progress.durationMs > 0)
                                            (progress.positionMs.toFloat() / progress.durationMs)
                                                .coerceIn(0f, 1f)
                                        else 0f
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .padding(start = 126.dp, end = 64.dp),
                                    color = Sunset.Accent,
                                )
                            }
                        }
                }
            }
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(tr("Clear history")) },
            text = { Text("Remove your watch history and saved playback positions?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClear()
                        confirm = false
                    }
                ) {
                    Text(tr("Clear"))
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(tr("Cancel")) } },
        )
}

@Composable
fun SavedScreen(
    catalog: Catalog,
    saved: Set<String>,
    onBack: () -> Unit,
    onShow: (String) -> Unit,
    onRemove: (String) -> Unit,
    onBrowse: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("all") }
    Column(Modifier.fillMaxSize()) {
        Heading("Saved", onBack)
        if (saved.isEmpty())
            BlankState(
                "Your watchlist is empty",
                "Found something you'll want back? Tap ＋ and it'll wait right here, out of the current.",
                "Find your next flow",
                onClick = onBrowse,
            )
        else {
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("all", "completed", "airing").forEach { status ->
                    TagChip(
                        if (status == "all") "All" else status.replaceFirstChar(Char::uppercase),
                        filter == status,
                    ) {
                        filter = status
                    }
                }
            }
            val shows =
                catalog.shows.filter { it.id in saved && (filter == "all" || it.status == filter) }
            if (shows.isEmpty())
                Text(tr("No results"), Modifier.padding(16.dp), color = Sunset.Text3)
            LazyColumn {
                items(shows, key = { it.id }, contentType = { "saved" }) { show ->
                    ShowRow(
                        show,
                        "${show.episodeCount} eps · ${show.status}",
                        { onShow(show.id) },
                    ) {
                        Glyph(Icons.Outlined.BookmarkRemove, "Remove", { onRemove(show.id) })
                    }
                }
            }
        }
    }
}
