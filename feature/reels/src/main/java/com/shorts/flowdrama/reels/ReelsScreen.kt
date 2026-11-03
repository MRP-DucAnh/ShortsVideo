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
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.CancellationException
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
    saved: Set<String>,
    onSave: (String) -> Unit,
    likedShows: Set<String>,
    onLike: (String) -> Unit,
) {
    val pager = rememberPagerState(pageCount = { catalog.shows.size })
    var episodes by remember { mutableStateOf<Map<String, Episode>>(emptyMap()) }
    var episodeLikes by remember(catalog) { mutableStateOf<Map<String, Long>>(emptyMap()) }
    LaunchedEffect(pager.settledPage, catalog, repository) {
        // Metadata for the next page is prepared ahead. Only the active page owns a decoder.
        listOf(pager.settledPage, pager.settledPage + 1).forEach { index ->
            catalog.shows.getOrNull(index)?.let { show ->
                if (show.id !in episodeLikes) {
                    try {
                        val loaded = repository.episodes(show.id)
                        loaded.firstOrNull { it.isFree }?.let {
                            episodes = episodes + (show.id to it)
                        }
                        episodeLikes = episodeLikes + (show.id to (loaded.maxOfOrNull { it.likeCount } ?: 0L))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Keep the poster visible when episode metadata is unavailable.
                    }
                }
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
        val liked = show.id in likedShows
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
                    loop = true,
                )
            Box(
                Modifier.fillMaxWidth()
                    .height(260.dp)
                    .align(Alignment.BottomCenter)
                    .background(Sunset.Scrim)
            )
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp).fillMaxWidth(.80f)) {
                Text(
                    episode?.displayTitle(showTitle(show)) ?: showTitle(show),
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
                    { onLike(show.id) },
                    if (liked) Sunset.Accent else Color.White,
                )
                val likes = show.likeCount?.takeIf { it > 0 }
                    ?.let { it + if (liked) 1 else 0 }
                    ?: episodeLikes[show.id]?.takeIf { it > 0 }
                if (likes != null) {
                    Text(formatLikes(likes, preferences.language), color = Sunset.Text,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
                Column(Modifier.clickable(onClick = onSpeed).padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    val speed = if (preferences.speed % 1f == 0f) preferences.speed.toInt().toString()
                        else preferences.speed.toString()
                    Text("${speed}×", color = Sunset.Text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text(tr("Speed"), color = Sunset.Text, fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Glyph(if (show.id in saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        if (show.id in saved) "Remove" else "Save", { onSave(show.id) }, Sunset.Text)
                    Text(tr(if (show.id in saved) "Saved" else "Save"), color = Sunset.Text, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun formatLikes(count: Long, language: String): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply {
        maximumFractionDigits = 1
    }
    return when {
        count >= 1_000_000 -> formatter.format(count / 1_000_000.0) + "M"
        count >= 1_000 -> formatter.format(count / 1_000.0) + "K"
        else -> formatter.format(count)
    }
}
