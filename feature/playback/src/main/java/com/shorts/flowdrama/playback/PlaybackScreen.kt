package com.shorts.flowdrama.playback

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.media3.exoplayer.ExoPlayer
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.player.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaybackScreen(
    show: Show,
    state: PlaybackState,
    preferences: Preferences,
    defaultLanguage: String,
    history: Progress?,
    onBack: () -> Unit,
    onEpisode: (Episode) -> Unit,
    onPreferences: ((Preferences) -> Preferences) -> Unit,
    onProgress: (Int, Long, Long) -> Unit,
    onLocked: (Episode) -> Unit,
    onCheckpoint: (Int, Long, Long) -> Unit = onProgress,
    blockedByAd: Boolean = false,
) {
    var sheet by rememberSaveable { mutableStateOf("") }
    var playing by rememberSaveable { mutableStateOf(true) }
    var liked by rememberSaveable { mutableStateOf(false) }
    val episode = state.current
    val source = episode?.let {
        PlaybackSourceResolver()
            .resolve(it, preferences.language, defaultLanguage, preferences.quality)
    }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (state.loading)
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Sunset.Accent)
        if (source != null && episode != null)
            key(show.id, episode.number, source.url) {
                val start = remember {
                    if (history?.episodeNumber == episode.number) history.positionMs else 0L
                }
                val next =
                    state.episodes
                        .getOrNull(state.episodes.indexOfFirst { it.number == episode.number } + 1)
                        ?.takeIf { it.isFree }
                val nextUrl = next?.let {
                    PlaybackSourceResolver()
                        .resolve(it, preferences.language, defaultLanguage, preferences.quality)
                        ?.url
                }
                VideoSurface(
                    source.url,
                    playing && !blockedByAd,
                    Modifier.fillMaxSize(),
                    speed = preferences.speed,
                    nextUrl = nextUrl,
                    startPosition = start,
                    onProgress = { position, duration ->
                        onProgress(episode.number, position, duration)
                    },
                    onPlayer = { player = it },
                    onCheckpoint = { p, d -> onCheckpoint(episode.number, p, d) },
                    onEnd = {
                        val index = state.episodes.indexOfFirst { it.number == episode.number }
                        val next = state.episodes.getOrNull(index + 1)
                        if (next != null && next.isFree) onEpisode(next)
                        else {
                            playing = false
                            if (next != null) onLocked(next)
                        }
                    },
                )
            }
        Box(
            Modifier.fillMaxWidth()
                .height(140.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = .6f), Color.Transparent)
                    )
                )
        )
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Glyph(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack, Color.White)
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        showTitle(show),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Ep. ${episode?.number ?: "–"} / ${state.episodes.size}",
                        color = Sunset.Text2,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (episode != null)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            showTitle(show),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        )
                        Text(
                            LocalizedTextResolver().resolve(show.hook, preferences.language),
                            color = Sunset.Text2,
                            fontSize = 13.sp,
                            maxLines = 3,
                        )
                        TextButton(onClick = { playing = !playing }) {
                            Text(tr(if (playing) "Pause" else "Play"), color = Color.White)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Glyph(
                            if (liked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            "Like",
                            { liked = !liked },
                            if (liked) Sunset.Accent else Color.White,
                        )
                        Text(
                            compactCount(
                                episode.likeCount + if (liked) 1 else 0,
                                preferences.audioLanguage ?: preferences.language,
                            ),
                            color = Color.White,
                            fontSize = 11.sp,
                        )
                        Box(
                            Modifier.size(48.dp)
                                .combinedClickable(
                                    onClick = {
                                        onPreferences {
                                            it.copy(
                                                speed =
                                                    when (it.speed) {
                                                        1f -> 1.5f
                                                        1.5f -> 2f
                                                        else -> 1f
                                                    }
                                            )
                                        }
                                    },
                                    onLongClick = { sheet = "options" },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${preferences.speed}×",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Glyph(Icons.Outlined.Apps, "Episodes", { sheet = "episodes" }, Color.White)
                        Glyph(
                            Icons.Outlined.Tune,
                            "Playback options",
                            { sheet = "options" },
                            Color.White,
                        )
                    }
                }
            player?.let { PlaybackTimeline(it) }
            TextButton(
                onClick = { sheet = "episodes" },
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    tr("Episodes") + "  ${episode?.number ?: "–"} / ${state.episodes.size}  ›",
                    color = Color.White,
                )
            }
        }
        if (source == null && !state.loading)
            Text(
                if (state.error) "Unable to load episodes" else "No playable source",
                Modifier.align(Alignment.Center),
                color = Color.White,
            )
    }
    if (sheet.isNotEmpty())
        ModalBottomSheet(
            onDismissRequest = { sheet = "" },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            if (sheet == "episodes") {
                Heading("Episodes")
                Text(
                    showTitle(show),
                    Modifier.padding(horizontal = 16.dp),
                    fontSize = 13.sp,
                    color = Sunset.Text3,
                )
                EpisodeGrid(
                    state.episodes,
                    episode?.number,
                    emptySet(),
                    Modifier.fillMaxWidth().heightIn(max = 450.dp),
                ) {
                    if (it.isFree) {
                        onEpisode(it)
                        sheet = ""
                        playing = true
                    } else {
                        sheet = ""
                        onLocked(it)
                    }
                }
            } else
                Column(Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
                    Heading("Playback options")
                    Text(tr("Speed"), fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            TagChip("${speed}×", preferences.speed == speed) {
                                onPreferences { it.copy(speed = speed) }
                            }
                        }
                    }
                    Text(tr("Quality"), Modifier.padding(top = 16.dp), fontWeight = FontWeight.Bold)
                    val qualities =
                        listOf("Auto") +
                            (episode?.sources?.get(source?.language)?.keys?.toList().orEmpty())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        qualities.forEach { quality ->
                            TagChip(quality, preferences.quality == quality) {
                                onPreferences { it.copy(quality = quality) }
                            }
                        }
                    }
                    Text(
                        tr("Audio language"),
                        Modifier.padding(top = 16.dp),
                        fontWeight = FontWeight.Bold,
                    )
                    episode?.sources?.keys?.forEach { code ->
                        TextButton(
                            onClick = {
                                onPreferences { it.copy(audioLanguage = code) }
                            }
                        ) {
                            Text(
                                (launchLanguages.find { it.first == code }?.second ?: code) +
                                    if (source?.language == code) "  ✓" else ""
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackTimeline(player: ExoPlayer) {
    var position by remember(player) { mutableLongStateOf(0) }
    var duration by remember(player) { mutableLongStateOf(0) }
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0)
            duration = player.duration.coerceAtLeast(0)
            delay(500)
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Slider(
            value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
            onValueChange = { player.seekTo((it * duration).toLong()) },
            enabled = duration > 0,
            thumb = { Box(Modifier.size(11.dp).background(Color.White, CircleShape)) },
            track = { slider ->
                Box(
                    Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = .25f))
                ) {
                    Box(
                        Modifier.fillMaxWidth(slider.value.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Sunset.Gradient)
                    )
                }
            },
            colors =
                SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Sunset.Accent),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(time(position), color = Sunset.Text2, fontSize = 11.sp)
            Text(time(duration), color = Sunset.Text2, fontSize = 11.sp)
        }
    }
}

private fun time(ms: Long) = "${ms/60000}:${(ms/1000%60).toString().padStart(2,'0')}"
