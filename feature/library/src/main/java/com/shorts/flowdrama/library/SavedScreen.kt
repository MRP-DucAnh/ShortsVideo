package com.shorts.flowdrama.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.feature.library.R
import java.util.Locale

@Composable
fun SavedScreen(catalog: Catalog, saved: Set<String>, onBack: () -> Unit, onShow: (String) -> Unit,
    onRemove: (String) -> Unit, onBrowse: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("all") }
    var editing by rememberSaveable { mutableStateOf(false) }
    val shows = catalog.shows.filter { show ->
        val status = show.status.lowercase(Locale.ROOT)
        show.id in saved && when (filter) {
            "airing" -> status == "airing" || status == "ongoing"
            "completed" -> status == "completed" || status == "complete"
            else -> true
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Glyph(FlowIcons.ArrowBack, libraryString(R.string.library_back), onBack)
            Text(libraryString(R.string.library_saved), Modifier.weight(1f), fontSize = 19.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { editing = !editing }, enabled = saved.isNotEmpty()) {
                Text(libraryString(if (editing) R.string.library_done else R.string.library_edit), color = Sunset.Text3)
            }
        }
        if (saved.isEmpty()) {
            BlankState(libraryString(R.string.library_saved_empty), libraryString(R.string.library_saved_empty_message),
                "Find your next flow", FlowIcons.Heart, onBrowse)
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all", "airing", "completed").forEach { status ->
                    val label = when (status) {
                        "all" -> libraryString(R.string.library_all) + " · ${saved.size}"
                        "airing" -> libraryString(R.string.library_airing)
                        else -> libraryString(R.string.library_complete)
                    }
                    TagChip(label, filter == status) { filter = status }
                }
            }
            if (shows.isEmpty()) Text(libraryString(R.string.library_no_results), Modifier.padding(16.dp), color = Sunset.Text3)
            LazyColumn {
                items(shows, key = { it.id }) { show ->
                    LibraryShowRow(show, libraryMetadata(show, catalog), { onShow(show.id) },
                        onRemove = { onRemove(show.id) }, editing = editing)
                }
            }
        }
    }
}

@Composable
internal fun LibraryEmpty(title: String, message: String, onBrowse: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(message, color = Sunset.Text3)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBrowse) { Text(libraryString(R.string.library_browse)) }
    }
}
