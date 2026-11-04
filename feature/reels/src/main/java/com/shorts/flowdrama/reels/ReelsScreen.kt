package com.shorts.flowdrama.reels

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.foundation.pager.*
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
    val online by com.shorts.flowdrama.network.rememberOnline()
    var playerOffline by remember { mutableStateOf(false) }
    var currentPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var failures by remember { mutableStateOf<Set<String>>(emptySet()) }
    val pager = rememberPagerState(pageCount = { catalog.shows.size })
    var episodes by remember { mutableStateOf<Map<String, Episode>>(emptyMap()) }
    var episodeLikes by remember(catalog) { mutableStateOf<Map<String, Long>>(emptyMap()) }
    LaunchedEffect(pager.settledPage, catalog, repository, online, retry) {
        // Metadata for the next page is prepared ahead. Only the active page owns a decoder.
        listOf(pager.settledPage, pager.settledPage + 1).forEach { index ->
            catalog.shows.getOrNull(index)?.let { show ->
                if (show.id !in episodeLikes) {
                    try {
                        failures = failures - show.id
                        val loaded = repository.episodes(show.id)
                        loaded.firstOrNull { it.isFree }?.let {
                            episodes = episodes + (show.id to it)
                        }
                        episodeLikes = episodeLikes + (show.id to (loaded.maxOfOrNull { it.likeCount } ?: 0L))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        failures = failures + show.id
                    }
                }
            }
        }
    }
    val missingMetadataOffline = !online && catalog.shows.getOrNull(pager.settledPage)?.id !in episodes
    var metadataOfflineVisible by remember { mutableStateOf(false) }
    LaunchedEffect(missingMetadataOffline, online) {
        if (missingMetadataOffline) metadataOfflineVisible = true
        else if (online) metadataOfflineVisible = false
    }
    val showOffline = playerOffline || missingMetadataOffline || metadataOfflineVisible
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        VerticalPager(
            state = pager,
            userScrollEnabled = !showOffline,
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
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                LoadingImage(
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
                        showOfflineOverlay = false,
                        onOfflineChanged = { playerOffline = it },
                        onPlayer = { currentPlayer = it },
                        backgroundColor = MaterialTheme.colorScheme.background,
                    )
                if (index == pager.settledPage && source == null) {
                    when {
                        !online -> Unit // The screen-level overlay covers video and all controls.
                        show.id in failures -> BlankState("Unable to load episodes", "Please try again.", "Try again", onClick = { retry++ })
                        show.id !in episodeLikes -> Skeleton(Modifier.fillMaxSize())
                        else -> Text(tr("Video unavailable"), Modifier.align(Alignment.Center), color = Color.White)
                    }
                }
                Box(
                    Modifier.fillMaxWidth()
                        .height(260.dp)
                        .align(Alignment.BottomCenter)
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = .65f), MaterialTheme.colorScheme.background)
                        ))
                )
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp).fillMaxWidth(.80f)) {
                    Text(
                        episode?.displayTitle(showTitle(show)) ?: showTitle(show),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        LocalizedTextResolver().resolve(show.hook, preferences.language),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(14.dp))
                    ActionButton(tr("Watch all %1\$d episodes", show.episodeCount), { onShow(show.id) })
                }
                Column(
                    Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 120.dp)
                        .background(
                            if (preferences.lightTheme) MaterialTheme.colorScheme.surface.copy(alpha = .9f) else Color.Transparent,
                            androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Glyph(
                        if (liked) FlowIcons.HeartFilled else FlowIcons.Heart,
                        "Like",
                        { onLike(show.id) },
                        if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        size = 29.dp,
                    )
                    val likes = show.likeCount?.takeIf { it > 0 }
                        ?.let { it + if (liked) 1 else 0 }
                        ?: episodeLikes[show.id]?.takeIf { it > 0 }
                    if (likes != null) {
                        Text(formatLikes(likes, preferences.language), color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.clickable(onClick = onSpeed).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        val speed = if (preferences.speed % 1f == 0f) preferences.speed.toInt().toString()
                            else preferences.speed.toString()
                        Text("${speed}×", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Text(tr("Speed"), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Glyph(if (show.id in saved) FlowIcons.BookmarkFilled else FlowIcons.Bookmark,
                            if (show.id in saved) "Remove" else "Save", { onSave(show.id) }, MaterialTheme.colorScheme.onSurface, size = 29.dp)
                        Text(tr(if (show.id in saved) "Saved" else "Save"), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }
            }
        }
        // Last sibling: dim and cover the video, captions and every Reels action together.
        if (showOffline) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .68f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
                MediaNetworkErrorState {
                    val player = currentPlayer
                    if (player != null) player.prepare() else retry++
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
