package com.shorts.flowdrama.reels

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.player.*

@Composable
fun ReelsScreen(
    catalog: Catalog,
    repository: CatalogRepository,
    preferences: Preferences,
    onShow: (String) -> Unit,
    onSpeed: () -> Unit,
) {
    val pager = rememberPagerState(pageCount = { catalog.shows.size })
    var episodes by remember { mutableStateOf<Map<String, Episode>>(emptyMap()) }
    LaunchedEffect(pager.settledPage) {
        // Metadata for the next page is prepared ahead. Only the active page owns a decoder.
        listOf(pager.settledPage, pager.settledPage + 1).forEach { index ->
            catalog.shows.getOrNull(index)?.let { show ->
                if (show.id !in episodes)
                    runCatching { repository.episodes(show.id).firstOrNull { it.isFree } }
                        .getOrNull()
                        ?.let { episodes = episodes + (show.id to it) }
            }
        }
    }
    VerticalPager(
        state = pager,
        key = { catalog.shows[it].id },
        modifier = Modifier.fillMaxSize(),
    ) { index ->
        val show = catalog.shows[index]
        val episode = episodes[show.id]
        val source = episode?.let {
            PlaybackSourceResolver()
                .resolve(it, preferences.language, catalog.defaultLanguage, preferences.quality)
        }
        var liked by remember(show.id) { mutableStateOf(false) }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                show.posterUrl,
                null,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            val nextShow = catalog.shows.getOrNull(index + 1)
            val nextUrl =
                nextShow
                    ?.let { episodes[it.id] }
                    ?.let {
                        PlaybackSourceResolver()
                            .resolve(
                                it,
                                preferences.audioLanguage ?: preferences.language,
                                catalog.defaultLanguage,
                                preferences.quality,
                            )
                            ?.url
                    }
            if (index == pager.settledPage && source != null)
                VideoSurface(
                    source.url,
                    true,
                    Modifier.fillMaxSize(),
                    speed = preferences.speed,
                    nextUrl = nextUrl,
                )
            Box(
                Modifier.fillMaxWidth()
                    .height(260.dp)
                    .align(Alignment.BottomCenter)
                    .background(Sunset.Scrim)
            )
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp).fillMaxWidth(.80f)) {
                Text(
                    showTitle(show),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(
                    LocalizedTextResolver().resolve(show.hook, preferences.language),
                    color = Sunset.Text2,
                    fontSize = 13.sp,
                    maxLines = 2,
                )
                Spacer(Modifier.height(14.dp))
                ActionButton("Watch all ${show.episodeCount} episodes", { onShow(show.id) })
            }
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Glyph(
                    if (liked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    "Like",
                    { liked = !liked },
                    if (liked) Sunset.Accent else Color.White,
                )
                TextButton(onClick = onSpeed) { Text("${preferences.speed}×", color = Color.White) }
            }
        }
    }
}
