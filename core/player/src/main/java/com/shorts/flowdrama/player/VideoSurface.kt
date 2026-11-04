package com.shorts.flowdrama.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.*
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.*
import com.shorts.flowdrama.network.rememberOnline
import com.shorts.flowdrama.network.hasInternetConnection
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.shorts.flowdrama.designsystem.MediaNetworkErrorState
import com.shorts.flowdrama.designsystem.tr
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.shorts.flowdrama.domain.PlaybackStatus
import com.shorts.flowdrama.domain.playbackStatus

fun isWifi(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return manager
        .getNetworkCapabilities(manager.activeNetwork)
        ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
}

/**
 * The composable owns the decoder. Every exit releases it; no Activity is retained in a ViewModel.
 */
@Composable
fun VideoSurface(
    url: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    speed: Float = 1f,
    startPosition: Long = 0,
    nextUrl: String? = null,
    onProgress: (Long, Long) -> Unit = { _, _ -> },
    onCheckpoint: (Long, Long) -> Unit = onProgress,
    onEnd: () -> Unit = {},
    onPlayer: (ExoPlayer?) -> Unit = {},
    loop: Boolean = false,
    onLoop: (Long) -> Unit = {},
    onWaiting: (Boolean) -> Unit = {},
    lowQualityUrl: String? = null,
    onVideoTap: (() -> Unit)? = null,
    showOfflineOverlay: Boolean = true,
    onOfflineChanged: (Boolean) -> Unit = {},
) {
    val observedOnline by rememberOnline()
    var online by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val currentActive by rememberUpdatedState(active)
    val currentCheckpoint by rememberUpdatedState(onCheckpoint)
    val currentProgress by rememberUpdatedState(onProgress)
    val currentEnd by rememberUpdatedState(onEnd)
    val currentLoop by rememberUpdatedState(onLoop)
    val currentPlayer by rememberUpdatedState(onPlayer)
    val currentOfflineChanged by rememberUpdatedState(onOfflineChanged)
    var checkpoint by rememberSaveable(url) { mutableLongStateOf(startPosition) }
    val player =
        remember(url) {
            // The API 37 headless emulator advertises a goldfish decoder which fails
            // while queueing codec config data. Use Android's software codec there;
            // real devices retain Media3's hardware-first selection.
            ExoPlayer.Builder(
                    context,
                    DefaultRenderersFactory(context)
                        .setEnableDecoderFallback(true)
                        .setMediaCodecSelector { mimeType, secure, tunneling ->
                            val codecs =
                                MediaCodecSelector.DEFAULT.getDecoderInfos(
                                    mimeType,
                                    secure,
                                    tunneling,
                                )
                            if (Build.HARDWARE in setOf("goldfish", "ranchu"))
                                codecs.sortedByDescending { it.softwareOnly }
                            else codecs
                        },
                )
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(context)
                        .setDataSourceFactory(MediaCache.factory(context))
                )
                .build()
                .apply {
                    setAudioAttributes(AudioAttributes.DEFAULT, !muted)
                    setHandleAudioBecomingNoisy(true)
                    volume = if (muted) 0f else 1f
                    setMediaItem(MediaItem.fromUri(url), checkpoint)
                    prepare()
                }
        }
    val prefetchScope = rememberCoroutineScope()
    DisposableEffect(nextUrl) {
        val writer = nextUrl?.let {
            CacheWriter(
                MediaCache.factory(context).createDataSource(),
                DataSpec.Builder().setUri(it).setLength(1024L * 1024L).build(),
                null,
                null,
            )
        }
        val job = writer?.let {
            prefetchScope.launch(Dispatchers.IO) { runCatching { it.cache() } }
        }
        onDispose {
            writer?.cancel()
            job?.cancel()
        }
    }
    var buffering by remember(player) { mutableStateOf(true) }
    var networkFailure by remember(player) { mutableStateOf(false) }
    var error by remember(player) { mutableStateOf(false) }
    var offlineVisible by remember(player) { mutableStateOf(false) }
    val currentOffline by rememberUpdatedState(offlineVisible)
    LaunchedEffect(offlineVisible) { currentOfflineChanged(offlineVisible) }
    // Re-read while actually stalled: callbacks/cached Flow values can lag radio changes.
    // Cached playback continues without an offline overlay until Media3 needs more data.
    LaunchedEffect(player, buffering, error, observedOnline, offlineVisible) {
        if (buffering || error || offlineVisible) {
            while (true) {
                online = hasInternetConnection(context)
                offlineVisible = playbackStatus(online, buffering, error, offlineVisible) == PlaybackStatus.Offline
                delay(500)
            }
        }
    }
    DisposableEffect(player, lifecycle) {
        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    buffering = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_READY) error = false
                    if (state == Player.STATE_ENDED) currentEnd()
                }

                override fun onPlayerError(problem: PlaybackException) {
                    networkFailure = problem.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        problem.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                    error = true
                    buffering = networkFailure
                }

                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                        currentLoop(player.duration.coerceAtLeast(0))
                    }
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        player.pause()
                        currentEnd()
                    }
                }
            }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                checkpoint = player.currentPosition
                currentCheckpoint(player.currentPosition, player.duration.coerceAtLeast(0))
                player.pause()
            }
            if (event == Lifecycle.Event.ON_START) player.playWhenReady = currentActive && !currentOffline
        }
        player.addListener(listener)
        lifecycle.lifecycle.addObserver(observer)
        currentPlayer(player)
        onDispose {
            checkpoint = player.currentPosition
            currentCheckpoint(player.currentPosition, player.duration.coerceAtLeast(0))
            currentPlayer(null)
            currentOfflineChanged(false)
            lifecycle.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }
    SideEffect {
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setPlaybackSpeed(speed)
        player.volume = if (muted) 0f else 1f
        player.playWhenReady =
            active && !offlineVisible && lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }
    LaunchedEffect(player) {
        while (true) {
            delay(1000)
            checkpoint = player.currentPosition
            currentProgress(player.currentPosition, player.duration.coerceAtLeast(0))
        }
    }
    var lowered by remember(player) { mutableStateOf(false) }
    var slow by remember(player) { mutableStateOf(false) }
    val currentWaiting by rememberUpdatedState(onWaiting)
    LaunchedEffect(buffering, error, offlineVisible) { currentWaiting(buffering || error || offlineVisible) }
    LaunchedEffect(buffering, online) {
        if (buffering && online) {
            delay(3000)
            slow = true
            if (!lowered && lowQualityUrl != null && lowQualityUrl != url) {
                lowered = true
                player.setMediaItem(MediaItem.fromUri(lowQualityUrl), player.currentPosition)
                player.prepare()
            }
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setMaxVideoSize(854, 480).build()
        }
    }
    // A failed source retains its current position; re-prepare once internet is validated again.
    LaunchedEffect(online) {
        if (online && error) { error = false; player.prepare() }
    }
    LaunchedEffect(error, online, active) {
        if (error && networkFailure && online && active) {
            delay(5000)
            error = false
            player.prepare()
        }
    }
    fun restoreHd() {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().clearVideoSizeConstraints().build()
        if (lowered) {
            lowered = false
            player.setMediaItem(MediaItem.fromUri(url), player.currentPosition)
            player.prepare()
        }
        slow = false
    }
    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    this.player = player
                }
            },
            update = {
                it.player = player
                it.keepScreenOn = active
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { it.player = null },
        )
        if (onVideoTap != null) Box(Modifier.fillMaxSize().clickable(enabled = !buffering && !error && !offlineVisible, onClick = onVideoTap))
        val status = playbackStatus(online, buffering, error && !networkFailure, offlineVisible)
        if (status == PlaybackStatus.Offline && showOfflineOverlay) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .65f))) {
                MediaNetworkErrorState {
                    online = hasInternetConnection(context)
                    if (online) { offlineVisible = false; error = false; player.prepare() }
                }
            }
        } else if (status == PlaybackStatus.Buffering) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)))
            Column(Modifier.align(Alignment.Center).testTag("SCR-0604").padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.size(44.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.height(18.dp))
                Text(tr("Loading…"), color = Color.White)
                if (slow) {
                    Spacer(Modifier.height(7.dp))
                    Text(tr(if ((player.videoFormat?.height ?: 0) in 1..480) "Slow network — playing at 480p or lower"
                        else "Slow network — trying a lower quality to keep playing"),
                        color = Color.White.copy(alpha = .6f), textAlign = TextAlign.Center)
                    HdRestoreButton(onClick = { restoreHd() }, modifier = Modifier.padding(top = 16.dp))
                }
            }
        } else if (status == PlaybackStatus.Unavailable) {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("Video unavailable. Try again."), color = Color.White)
                TextButton(onClick = { error = false; player.prepare() }) { Text(tr("Try again"), color = Color.White) }
            }
        }
    }
}

@Composable
private fun HdRestoreButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = .14f), contentColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .22f)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)) {
        Text(tr("Switch back to HD"), color = Color.White, fontSize = 13.sp)
    }
}
