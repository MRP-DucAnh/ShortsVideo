package com.shorts.flowdrama.search

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(catalog: Catalog, onBack: () -> Unit, onShow: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val term = query.trim()
    LaunchedEffect(term) {
        delay(300)
        debounced = term
    }
    val language = LocalLanguage.current
    val resolver = remember { LocalizedTextResolver() }
    val suggestions = remember(catalog, term, language) {
        val candidates = catalog.shows.map {
            resolver.resolve(it.title, language, catalog.defaultLanguage)
        } + catalog.categories.map {
            resolver.resolve(it.name, language, catalog.defaultLanguage)
        }
        candidates.filter { it.isNotBlank() && it.contains(term, ignoreCase = true) }
            .distinct().take(if (term.isEmpty()) 8 else 4)
    }
    // Hide stale results immediately when editing, including when deleting below two letters.
    val ready = term.length >= 2 && term == debounced
    val results = remember(catalog, debounced, language, ready) {
        if (ready) SearchShows()(catalog, debounced, language) else emptyList()
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Glyph(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Sunset.Text, fontSize = 15.sp),
                cursorBrush = SolidColor(Sunset.Accent),
                modifier = Modifier.weight(1f).height(44.dp)
                    .onFocusChanged { focused = it.isFocused }
                    .background(Sunset.Surface2, RoundedCornerShape(999.dp))
                    .border(1.dp, if (focused || term.isNotEmpty()) Sunset.Accent else Sunset.Border,
                        RoundedCornerShape(999.dp)),
                decorationBox = { field ->
                    Row(Modifier.fillMaxSize().padding(start = 14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Search, null, Modifier.size(18.dp), tint = Sunset.Text3)
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text(tr("Search dramas"), color = Sunset.Text3,
                                fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            field()
                        }
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Outlined.Close, tr("Clear"), Modifier.size(18.dp), tint = Sunset.Text3)
                            }
                        } else Spacer(Modifier.width(14.dp))
                    }
                },
            )
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (term.isEmpty()) item { SectionTitle("Trending searches") }
            items(suggestions, key = { "suggestion:$it" }, contentType = { "suggestion" }) { suggestion ->
                Row(
                    Modifier.fillMaxWidth().clickable { query = suggestion }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(17.dp), tint = Sunset.Text3)
                    val highlighted = buildAnnotatedString {
                        append(suggestion)
                        val start = suggestion.indexOf(term, ignoreCase = true)
                        if (term.isNotEmpty() && start >= 0) addStyle(
                            SpanStyle(color = Sunset.Text, fontWeight = FontWeight.SemiBold),
                            start, start + term.length,
                        )
                    }
                    Text(highlighted, Modifier.weight(1f), color = Sunset.Text2, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("↖", color = Sunset.Text4, fontSize = 15.sp)
                }
            }
            if (ready) {
                item { SectionTitle("Matches") }
                if (results.isEmpty()) item {
                    BlankState("No results", "Try another title or genre.", "Clear", Icons.Outlined.Search) {
                        query = ""
                    }
                }
                items(results, key = { "show:${it.id}" }, contentType = { "show" }) { show ->
                    val genre = catalog.categories.firstOrNull { it.id in show.tagIds }?.let {
                        resolver.resolve(it.name, language, catalog.defaultLanguage)
                    }
                    val subtitle = listOfNotNull(genre, "${show.episodeCount} ${tr("eps")}").joinToString(" · ")
                    Row(
                        Modifier.fillMaxWidth().clickable { onShow(show.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Poster(show, Modifier.size(98.dp, 60.dp)) { onShow(show.id) }
                        Column(Modifier.weight(1f).height(60.dp).clipToBounds(),
                            verticalArrangement = Arrangement.Center) {
                            Text(resolver.resolve(show.title, language, catalog.defaultLanguage),
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                            Spacer(Modifier.height(4.dp))
                            Text(subtitle, fontSize = 12.sp, color = Sunset.Text3,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                        }
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp), tint = Sunset.Text3)
                    }
                }
            }
        }
    }
}
