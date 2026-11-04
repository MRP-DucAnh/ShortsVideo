package com.shorts.flowdrama.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.feature.library.R

@Composable
fun HistoryScreen(catalog: Catalog, history: List<Progress>?, onBack: () -> Unit,
    onEpisode: (String, Int) -> Unit, onRemove: (String, Int) -> Unit, onClear: () -> Unit, onBrowse: () -> Unit) {
    var finished by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val shows = remember(catalog) { catalog.shows.associateBy { it.id } }
    val entries = history.orEmpty()
    val filtered = entries.filter { it.watched == finished && it.showId in shows }
        .sortedByDescending { it.updatedAt }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Glyph(Icons.AutoMirrored.Outlined.ArrowBack, libraryString(R.string.library_back), onBack)
            Text(libraryString(R.string.library_watch_history), Modifier.weight(1f), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            if (editing) TextButton(onClick = { confirm = true }, enabled = entries.isNotEmpty()) {
                Text(libraryString(R.string.library_clear), color = Sunset.Text3)
            }
            TextButton(onClick = { editing = !editing }, enabled = entries.isNotEmpty()) {
                Text(libraryString(if (editing) R.string.library_done else R.string.library_edit), color = Sunset.Text3)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(999.dp)).background(Sunset.Surface2).padding(3.dp)) {
            listOf(R.string.library_watching, R.string.library_finished).forEachIndexed { index, id ->
                val selected = finished == (index == 1)
                Text(libraryString(id), Modifier.weight(1f).clip(RoundedCornerShape(999.dp))
                    .background(if (selected) Sunset.Surface3 else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { finished = index == 1 }.padding(horizontal = 8.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center, maxLines = 1,
                    color = if (selected) Sunset.Text else Sunset.Text3,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (history == null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Skeleton(Modifier.size(98.dp, 64.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Skeleton(Modifier.fillMaxWidth(.8f).height(16.dp))
                            Skeleton(Modifier.fillMaxWidth(.55f).height(12.dp))
                        }
                    }
                }
            }
        } else if (filtered.isEmpty()) {
            val title = if (entries.isEmpty()) R.string.library_history_empty else R.string.library_no_results
            val message = when {
                entries.isEmpty() -> R.string.library_history_empty_message
                finished -> R.string.library_finished_empty
                else -> R.string.library_watching_empty
            }
            LibraryEmpty(libraryString(title), libraryString(message), onBrowse)
        } else LazyColumn(Modifier.padding(top = 8.dp)) {
            items(filtered, key = { "${it.showId}:${it.episodeNumber}" }) { progress ->
                val show = shows.getValue(progress.showId)
                LibraryShowRow(show, progressSubtitle(progress, show, showCompleted = progress.watched),
                    { onEpisode(show.id, progress.episodeNumber) }, progress, true,
                    onRemove = { onRemove(show.id, progress.episodeNumber) }, editing = editing)
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text(libraryString(R.string.library_clear_history)) },
        text = { Text(libraryString(R.string.library_clear_confirmation)) },
        confirmButton = { TextButton(onClick = { onClear(); confirm = false }) { Text(libraryString(R.string.library_clear)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(libraryString(R.string.library_cancel)) } })
}
