package com.shorts.flowdrama.playback

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.media3.exoplayer.ExoPlayer
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.player.*
import com.shorts.flowdrama.feature.playback.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.layout.ContentScale

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
    onEpisodeComplete: (Int, Long) -> Unit = { _, _ -> },
    blockedByAd: Boolean = false,
    saved: Boolean = false,
    onSave: () -> Unit = {},
    episodeProgress: List<Progress> = emptyList(),
    progressReady: Boolean = true,
    likedEpisodes: Set<EpisodeKey> = emptySet(),
    onLikeEpisode: (String, Int) -> Unit = { _, _ -> },
    onRetry: () -> Unit = {},
) {
    val online by com.shorts.flowdrama.network.rememberOnline()
    LaunchedEffect(online) { if (online && state.error) onRetry() }
    var sheet by rememberSaveable { mutableStateOf("") }
    var playing by rememberSaveable { mutableStateOf(true) }

    var feedback by remember { mutableStateOf<Boolean?>(null) }
    var feedbackCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(feedbackCount) {
        if (feedbackCount > 0) {
            delay(900)
            feedback = null
        }
    }
    val context = LocalContext.current
    val audioPreferences = remember(context) { PlaybackAudioPreferences(context) }
    var audioLanguage by remember(audioPreferences) { mutableStateOf(audioPreferences.language) }
    val videoLanguage = audioLanguage ?: defaultLanguage
    val episode = state.current
    val source = episode?.let {
        PlaybackSourceResolver().resolve(it, videoLanguage, defaultLanguage, preferences.quality)
    }
    val selectedIndex = state.episodes.indexOfFirst { it.number == episode?.number }
    val pager = rememberPagerState(initialPage = selectedIndex.coerceAtLeast(0), pageCount = { state.episodes.size })
    val latestState by rememberUpdatedState(state)
    val latestOnEpisode by rememberUpdatedState(onEpisode)
    val latestOnLocked by rememberUpdatedState(onLocked)
    val latestBlocked by rememberUpdatedState(blockedByAd)

    // A selection from the episode sheet or a restored route also moves the pager.
    LaunchedEffect(episode?.number, state.episodes) {
        if (selectedIndex >= 0 && pager.settledPage != selectedIndex) pager.scrollToPage(selectedIndex)
    }
    LaunchedEffect(pager, show.id) {
        snapshotFlow { pager.settledPage }.drop(1).collect { page ->
            val current = latestState.current
            val target = latestState.episodes.getOrNull(page) ?: return@collect
            if (target.number == current?.number) return@collect
            if (latestBlocked || !target.isFree) {
                val currentPage = latestState.episodes.indexOfFirst { it.number == current?.number }
                if (currentPage >= 0) pager.scrollToPage(currentPage)
                if (!latestBlocked && !target.isFree) latestOnLocked(target)
            } else {
                playing = true
                latestOnEpisode(target)
            }
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !blockedByAd && progressReady && sheet.isEmpty(),
            key = { state.episodes[it].number },
        ) { page ->
            val episode = state.episodes[page]
            val active = page == pager.settledPage && episode.number == state.current?.number && episode.isFree
            val liked = EpisodeKey(show.id, episode.number) in likedEpisodes
            val source = PlaybackSourceResolver().resolve(episode, videoLanguage, defaultLanguage, preferences.quality)
            val playDescription = playbackString(if (playing) R.string.player_pause else R.string.player_play)
            var waiting by remember(episode.number) { mutableStateOf(false) }
            var player by remember(episode.number) { mutableStateOf<ExoPlayer?>(null) }
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                LoadingImage(show.posterUrl, null, Modifier.fillMaxSize().background(artwork(show.id)), contentScale = ContentScale.Crop)
                if (source != null && active && progressReady)
                    key(show.id, episode.number, source.url) {
                        val start = remember {
                            val progress = episodeProgress.find { it.episodeNumber == episode.number }
                                ?: history?.takeIf { it.episodeNumber == episode.number }
                            if (progress != null && progress.durationMs > 0 && progress.positionMs >= progress.durationMs) 0L
                            else progress?.positionMs ?: 0L
                        }
                        val next =
                            state.episodes
                                .getOrNull(state.episodes.indexOfFirst { it.number == episode.number } + 1)
                                ?.takeIf { it.isFree }
                        val nextUrl = next?.let {
                            PlaybackSourceResolver()
                                .resolve(it, videoLanguage, defaultLanguage, preferences.quality)
                                ?.url
                        }
                        VideoSurface(
                            source.url,
                            playing && !blockedByAd,
                            Modifier.fillMaxSize().testTag("playback:video-surface")
                                .semantics { contentDescription = playDescription },
                            onVideoTap = {
                                if (!blockedByAd) {
                                    playing = !playing
                                    feedback = playing
                                    feedbackCount++
                                }
                            },
                            speed = preferences.speed,
                            nextUrl = nextUrl,
                            startPosition = start,
                            onProgress = { position, duration ->
                                onProgress(episode.number, position, duration)
                            },
                            onPlayer = { player = it },
                            onWaiting = { waiting = it },
                            lowQualityUrl = episode.sources[source.language]?.entries
                                ?.filter { (quality, url) ->
                                    (quality.filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE) <= minOf(480, source.quality.filter(Char::isDigit).toIntOrNull() ?: 480) && url.startsWith("https://")
                                }?.maxByOrNull { it.key.filter(Char::isDigit).toIntOrNull() ?: 0 }?.value,

                            onCheckpoint = { p, d -> onCheckpoint(episode.number, p, d) },
                            loop = true,
                            onLoop = { duration ->
                                onEpisodeComplete(episode.number, duration)
                            },
                        )
                    }
                feedback?.let { isPlaying ->
                    Icon(
                        if (isPlaying) FlowIcons.Play else FlowIcons.Pause,
                        null,
                        Modifier.align(Alignment.Center).size(64.dp)
                            .background(Color.Black.copy(alpha = .35f), CircleShape).padding(16.dp),
                        tint = Color.White,
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
                        Glyph(FlowIcons.ArrowBack, "Back", onBack, Color.White)
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
                                playbackString(R.string.player_episode_position, episode?.number?.toString() ?: "–", state.episodes.size),
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
                                    episode.displayTitle(showTitle(show)),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    LocalizedTextResolver().resolve(show.hook, preferences.language),
                                    color = Sunset.Text2,
                                    fontSize = 12.sp,
                                    maxLines = 3,
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Glyph(
                                    if (liked) FlowIcons.HeartFilled else FlowIcons.Heart,
                                    "Like",
                                    { onLikeEpisode(show.id, episode.number) },
                                    if (liked) Sunset.Accent else Color.White,
                        size = 29.dp,
                                )
                                Text(
                                    compactCount(
                                        episode.likeCount + if (liked) 1 else 0,
                                        preferences.audioLanguage ?: preferences.language,
                                    ),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier.size(48.dp).combinedClickable(
                                            onClick = {
                                                onPreferences { it.copy(speed = when (it.speed) {
                                                    1f -> 1.5f
                                                    1.5f -> 2f
                                                    else -> 1f
                                                }) }
                                            },
                                            onLongClick = { sheet = "options" },
                                        ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "${if (preferences.speed % 1f == 0f) preferences.speed.toInt().toString() else preferences.speed.toString()}×",
                                            color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                                        )
                                    }
                                    Text(playbackString(R.string.player_speed), color = Color.White, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Glyph(
                                        if (saved) FlowIcons.BookmarkFilled else FlowIcons.Bookmark,
                                        if (saved) "Remove" else "Save", onSave, Color.White,
                                        size = 29.dp,
                                    )
                                    Text(playbackString(if (saved) R.string.player_saved else R.string.player_save), color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    player?.let { PlaybackTimeline(it, waiting) }
                    Box(
                        Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
                            .heightIn(min = 48.dp)
                            .clickable { sheet = "episodes" },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            Modifier.clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = .14f))
                                .border(1.dp, Color.White.copy(alpha = .2f), RoundedCornerShape(999.dp))
                                .heightIn(min = 32.dp)
                                .padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Icon(FlowIcons.Menu, null, Modifier.size(16.dp), tint = Color.White)
                            Text(playbackString(R.string.player_episodes), color = Color.White, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
                if (source == null && active && !state.loading)
                    Text(
                        playbackString(if (state.error) R.string.player_load_error else R.string.player_no_source),
                        Modifier.align(Alignment.Center),
                        color = Color.White,
                    )
            }
        }
        if (!online && state.current == null) MediaNetworkErrorState(onRetry)
        else if (state.error) BlankState("Unable to load episodes", "Please try again.", "Try again", onClick = onRetry)
        else if (state.loading || !progressReady) Skeleton(Modifier.fillMaxSize())
    }
    if (sheet == "episodes") {
        EpisodePickerSheet(
            episodes = state.episodes,
            current = episode?.number,
            watched = episodeProgress.filter { it.watched }.map { it.episodeNumber }.toSet(),
            onDismiss = { sheet = "" },
            onEpisode = {
                sheet = ""
                if (it.isFree) {
                    onEpisode(it)
                    playing = true
                } else onLocked(it)
            },
        )
    } else if (sheet == "options") {
        PlaybackOptionsSheet(
            episode = episode,
            source = source,
            preferences = preferences,
            onDismiss = { sheet = "" },
            onPreferences = onPreferences,
            onAudioLanguage = { language ->
                audioPreferences.language = language
                audioLanguage = language
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackTimeline(player: ExoPlayer, waiting: Boolean) {
    var position by remember(player) { mutableLongStateOf(0) }
    var duration by remember(player) { mutableLongStateOf(0) }
    var dragging by remember(player) { mutableStateOf(false) }
    var scrub by remember(player) { mutableFloatStateOf(0f) }
    LaunchedEffect(player) {
        while (true) {
            if (!dragging) position = player.currentPosition.coerceAtLeast(0)
            duration = player.duration.coerceAtLeast(0)
            delay(100)
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp).alpha(if (waiting) .45f else 1f)) {
        PlaybackSeekSlider(
            value = if (dragging) scrub else if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
            onValueChange = { dragging = true; scrub = it; position = (it * duration).toLong() },
            onValueChangeFinished = { player.seekTo((scrub * duration).toLong()); dragging = false },
            enabled = duration > 0 && !waiting,
        )
        PlaybackTimeLabels(position, duration)
    }
}

@Composable
internal fun PlaybackTimeLabels(position: Long, duration: Long) {
    // Slider reserves half the 11 dp thumb width at each end for its center.
    Row(Modifier.fillMaxWidth().padding(horizontal = 5.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(time(position), Modifier.testTag("playback:elapsed"), color = Sunset.Text2, fontSize = 11.sp)
        Text(time(duration), Modifier.testTag("playback:duration"), color = Sunset.Text2, fontSize = 11.sp)
    }
}

private fun time(ms: Long) = "${ms/60000}:${(ms/1000%60).toString().padStart(2,'0')}"


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaybackSeekSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    enabled: Boolean,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        modifier = Modifier.fillMaxWidth().height(32.dp),
        enabled = enabled,
        thumb = {
            Box(Modifier.width(11.dp).height(32.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(11.dp).testTag("playback:thumb").background(Color.White, CircleShape))
            }
        },
        track = { slider ->
            Box(Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.CenterStart) {
                Box(Modifier.fillMaxWidth().height(3.dp).testTag("playback:track")
                    .clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = .25f))) {
                    Box(Modifier.fillMaxWidth(slider.value.coerceIn(0f, 1f))
                        .fillMaxHeight().background(Sunset.Gradient))
                }
            }
        },
        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Sunset.Accent),
    )
}
