package com.shorts.flowdrama.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.foundation.background
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
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val currentActive by rememberUpdatedState(active)
    val currentCheckpoint by rememberUpdatedState(onCheckpoint)
    val currentProgress by rememberUpdatedState(onProgress)
    val currentEnd by rememberUpdatedState(onEnd)
    val currentPlayer by rememberUpdatedState(onPlayer)
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
    var error by remember(player) { mutableStateOf(false) }
    DisposableEffect(player, lifecycle) {
        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    buffering = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_ENDED) currentEnd()
                }

                override fun onPlayerError(problem: PlaybackException) {
                    error = true
                    buffering = false
                }

                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
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
            if (event == Lifecycle.Event.ON_START) player.playWhenReady = currentActive
        }
        player.addListener(listener)
        lifecycle.lifecycle.addObserver(observer)
        currentPlayer(player)
        onDispose {
            checkpoint = player.currentPosition
            currentCheckpoint(player.currentPosition, player.duration.coerceAtLeast(0))
            currentPlayer(null)
            lifecycle.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }
    SideEffect {
        player.setPlaybackSpeed(speed)
        player.volume = if (muted) 0f else 1f
        player.playWhenReady =
            active && lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }
    LaunchedEffect(player) {
        while (true) {
            delay(1000)
            checkpoint = player.currentPosition
            currentProgress(player.currentPosition, player.duration.coerceAtLeast(0))
        }
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
        if (buffering)
            CircularProgressIndicator(
                Modifier.align(Alignment.Center).size(36.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        if (error)
            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Video unavailable. Check your connection.", color = Color.White)
                TextButton(
                    onClick = {
                        error = false
                        player.prepare()
                        player.playWhenReady = active
                    }
                ) {
                    Text("Try again", color = Color.White)
                }
            }
    }
}
