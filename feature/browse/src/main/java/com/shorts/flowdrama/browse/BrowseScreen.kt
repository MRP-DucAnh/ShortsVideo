package com.shorts.flowdrama.browse

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.player.*

@Composable
fun BrowseScreen(
    catalog: Catalog,
    preferences: Preferences,
    saved: Set<String>,
    repository: CatalogRepository,
    onPlay: (String, Int) -> Unit,
    onSave: (String) -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf(catalog.shows.firstOrNull()?.id.orEmpty()) }
    var filter by rememberSaveable { mutableStateOf("all") }
    val selected = catalog.shows.find { it.id == selectedId }
    val filtered =
        remember(catalog, filter) {
            if (filter == "all") catalog.shows else catalog.shows.filter { filter in it.tagIds }
        }
    var preview by remember(selectedId) { mutableStateOf<Episode?>(null) }
    val context = LocalContext.current
    val previewAllowed = !preferences.wifiPreviewOnly || isWifi(context)
    LaunchedEffect(selectedId, previewAllowed) {
        if (previewAllowed && selectedId.isNotEmpty())
            preview =
                runCatching { repository.episodes(selectedId).firstOrNull { it.isFree } }
                    .getOrNull()
    }
    val source = preview?.let {
        PlaybackSourceResolver().resolve(it, preferences.language, catalog.defaultLanguage, "Auto")
    }
    Column(Modifier.fillMaxSize()) {
        if (selected != null)
            Box(
                Modifier.fillMaxWidth()
                    .weight(.53f)
                    .heightIn(min = 220.dp)
                    .background(artwork(selected.id))
            ) {
                AsyncImage(
                    selected.posterUrl,
                    null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (source != null && previewAllowed)
                    VideoSurface(source.url, true, Modifier.fillMaxSize(), muted = true, onEnd = {})
                Box(Modifier.fillMaxSize().background(Sunset.Scrim))
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(tr("Browse"), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (previewAllowed) "PREVIEW · auto" else "PREVIEW · Wi-Fi only",
                        fontSize = 10.sp,
                        color = Sunset.Text2,
                    )
                }
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(
                        showTitle(selected),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Sunset.Text,
                    )
                    Text(
                        "${selected.tagIds.take(2).joinToString(" · ")} · ${selected.episodeCount} eps · ${selected.status}",
                        fontSize = 12.sp,
                        color = Sunset.Text2,
                    )
                    Row(
                        Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ActionButton("Play Ep 1", { onPlay(selected.id, -1) }, Modifier.weight(1f))
                        Glyph(
                            if (selected.id in saved) Icons.Outlined.Bookmark
                            else Icons.Outlined.Add,
                            "Save",
                            { onSave(selected.id) },
                            Sunset.Text,
                        )
                    }
                }
            }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TagChip("All", filter == "all") { filter = "all" }
            catalog.categories.forEach { tag ->
                TagChip(
                    LocalizedTextResolver().resolve(tag.name, LocalLanguage.current),
                    filter == tag.id,
                ) {
                    filter = tag.id
                }
            }
        }
        Text(
            tr("Tap a poster · preview changes above"),
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            fontSize = 10.sp,
            color = Sunset.Text3,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(.47f),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 16.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            items(filtered, key = { it.id }, contentType = { "poster" }) { show ->
                Poster(
                    show,
                    Modifier.fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .alpha(if (show.id == selectedId) 1f else .72f),
                    selected = show.id == selectedId,
                ) {
                    selectedId = show.id
                }
            }
        }
    }
}
