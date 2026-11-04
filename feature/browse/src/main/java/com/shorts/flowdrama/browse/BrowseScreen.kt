package com.shorts.flowdrama.browse

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.feature.browse.R
import com.shorts.flowdrama.player.*
import java.util.Locale
import kotlinx.coroutines.CancellationException

@Composable
private fun browseString(id: Int, vararg args: Any): String {
    val context = LocalContext.current
    val language = LocalLanguage.current
    val configuration = LocalConfiguration.current
    val localized = remember(context, language, configuration) {
        context.createConfigurationContext(Configuration(configuration).apply {
            setLocale(Locale.forLanguageTag(language))
        })
    }
    return localized.getString(id, *args)
}

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
    val filtered = remember(catalog, filter) {
        if (filter == "all") catalog.shows else catalog.shows.filter { filter in it.tagIds }
    }
    var preview by remember(selectedId) { mutableStateOf<Episode?>(null) }
    val context = LocalContext.current
    val previewAllowed = !preferences.wifiPreviewOnly || isWifi(context)
    LaunchedEffect(selectedId, previewAllowed, repository) {
        preview = null
        if (previewAllowed && selectedId.isNotEmpty()) {
            preview = try {
                repository.episodes(selectedId).firstOrNull { it.isFree }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }
    val source = preview?.let {
        PlaybackSourceResolver().resolve(it, preferences.language, catalog.defaultLanguage, "Auto")
    }
    Column(Modifier.fillMaxSize().background(Sunset.Background)) {
        if (selected != null) {
            PreviewDock(
                selected, catalog, source?.url?.takeIf { previewAllowed }, previewAllowed,
                selected.id in saved, { onPlay(selected.id, -1) }, { onSave(selected.id) },
                Modifier.fillMaxWidth().weight(.53f).heightIn(min = 220.dp),
            )
        }
        BrowseFilters(catalog, filter) { filter = it }
        Text(
            browseString(R.string.browse_poster_hint).uppercase(Locale.forLanguageTag(LocalLanguage.current)),
            Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
            fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = .66.sp,
            color = Sunset.Text3,
        )
        PosterGrid(filtered, selectedId, { selectedId = it }, Modifier.weight(.47f))
    }
}

@Composable
private fun PreviewDock(
    show: Show,
    catalog: Catalog,
    previewUrl: String?,
    previewAllowed: Boolean,
    saved: Boolean,
    onPlay: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(artwork(show.id))) {
        LoadingImage(show.posterUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (previewUrl != null) {
            VideoSurface(previewUrl, true, Modifier.fillMaxSize(), muted = true, loop = true)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Sunset.Background.copy(alpha = .35f),
            .26f to Color.Transparent,
            .52f to Color.Transparent,
            .92f to Sunset.Background.copy(alpha = .94f),
            1f to Sunset.Background,
        )))
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(browseString(R.string.browse_title), fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold, color = Sunset.Text)
            Text(
                browseString(if (previewAllowed) R.string.browse_preview_auto else R.string.browse_preview_wifi),
                Modifier.background(Color.White.copy(alpha = .14f), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = .20f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = .6.sp, color = Sunset.Text,
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
            Text(showTitle(show), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold,
                color = Sunset.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                showMetadata(show, catalog), Modifier.padding(top = 3.dp),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Sunset.Text2,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            PreviewActions(saved, onPlay, onSave)
        }
    }
}

@Composable
private fun showMetadata(show: Show, catalog: Catalog): String {
    val language = LocalLanguage.current
    val resolver = remember { LocalizedTextResolver() }
    val genre = catalog.categories.firstOrNull { it.id in show.tagIds }?.let {
        resolver.resolve(it.name, language, catalog.defaultLanguage)
    }
    val status = when (show.status.lowercase(Locale.ROOT)) {
        "completed", "complete" -> browseString(R.string.browse_complete)
        "ongoing" -> browseString(R.string.browse_ongoing)
        else -> null
    }
    return listOfNotNull(genre, browseString(R.string.browse_episode_count, show.episodeCount), status)
        .joinToString(" · ")
}

@Composable
private fun PreviewActions(saved: Boolean, onPlay: () -> Unit, onSave: () -> Unit) {
    Row(Modifier.padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).heightIn(min = 42.dp).clip(RoundedCornerShape(12.dp))
                .background(Sunset.Gradient).clickable(onClick = onPlay)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(FlowIcons.Play, null, Modifier.size(20.dp), tint = Sunset.OnAccent)
            Spacer(Modifier.width(8.dp))
            Text(browseString(R.string.browse_play), fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold, color = Sunset.OnAccent,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onSave),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(42.dp).background(Color.White.copy(alpha = .12f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = .18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Icon(if (saved) FlowIcons.BookmarkFilled else FlowIcons.Add,
                    browseString(if (saved) R.string.browse_remove_saved else R.string.browse_save),
                    Modifier.size(20.dp), tint = Sunset.Text)
            }
        }
    }
}

@Composable
private fun BrowseFilters(catalog: Catalog, filter: String, onFilter: (String) -> Unit) {
    val language = LocalLanguage.current
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BrowseChip(browseString(R.string.browse_all), filter == "all") { onFilter("all") }
        catalog.categories.forEach { tag ->
            BrowseChip(LocalizedTextResolver().resolve(tag.name, language, catalog.defaultLanguage), filter == tag.id) {
                onFilter(tag.id)
            }
        }
    }
}

@Composable
private fun BrowseChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Text(label, Modifier.clip(shape).background(if (selected) Sunset.Surface3 else Color.Transparent)
        .then(if (selected) Modifier else Modifier.border(1.dp, Sunset.Border, shape))
        .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        color = if (selected) Sunset.Text else Sunset.Text2,
        fontSize = 11.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
}

@Composable
private fun PosterGrid(shows: List<Show>, selectedId: String, onSelect: (String) -> Unit, modifier: Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4), modifier = modifier,
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(shows, key = { it.id }, contentType = { "poster" }) { show ->
            Poster(show, Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .alpha(if (show.id == selectedId) 1f else .72f), selected = show.id == selectedId) {
                onSelect(show.id)
            }
        }
    }
}
