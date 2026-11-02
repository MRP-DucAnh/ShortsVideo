package com.shorts.flowdrama.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(catalog: Catalog, onBack: () -> Unit, onShow: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(300)
        debounced = query
    }
    val language = LocalLanguage.current
    val results =
        remember(catalog, debounced, language) { SearchShows()(catalog, debounced, language) }
    Column(Modifier.fillMaxSize().imePadding()) {
        Heading("Search", onBack)
        OutlinedTextField(
            query,
            { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            placeholder = { Text(tr("Search dramas")) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) Glyph(Icons.Outlined.Close, "Clear", { query = "" })
            },
        )
        if (debounced.isBlank())
            LazyColumn {
                item { SectionTitle("Trending searches") }
                items(
                    catalog.categories.take(8),
                    key = { it.id },
                    contentType = { "suggestion" },
                ) { tag ->
                    TextButton(
                        onClick = { query = LocalizedTextResolver().resolve(tag.name, language) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(LocalizedTextResolver().resolve(tag.name, language))
                    }
                }
            }
        else if (results.isEmpty())
            BlankState(
                "No results",
                "Try another title or genre.",
                "Clear",
                Icons.Outlined.Search,
            ) {
                query = ""
            }
        else
            LazyColumn {
                item { SectionTitle("Matches") }
                items(results, key = { it.id }, contentType = { "show" }) { show ->
                    ShowRow(
                        show,
                        "${show.episodeCount} eps · ${show.tagIds.take(2).joinToString(" · ")}",
                        { onShow(show.id) },
                    )
                }
            }
    }
}
