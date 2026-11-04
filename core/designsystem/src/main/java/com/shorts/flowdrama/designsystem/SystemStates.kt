package com.shorts.flowdrama.designsystem

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.shorts.flowdrama.network.openInternetSettings

/** SCR-0601: dimensions are owned by the final layout. Respect system Reduce Motion. */
@Composable
fun Skeleton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val motion = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val phase = if (motion) {
        val transition = rememberInfiniteTransition(label = "skeleton")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "shimmer").value
    } else 0f
    Box(modifier.testTag("SCR-0601").clip(RoundedCornerShape(10.dp))
        .background(Brush.linearGradient(listOf(base, if (motion) highlight.copy(alpha = .5f + phase * .5f) else base, base))))
}

/** Shared image loading/error treatment for every content artwork. */
@Composable
fun LoadingImage(model: Any?, contentDescription: String?, modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop) {
    val online by com.shorts.flowdrama.network.rememberOnline()
    var attempt by remember(model) { mutableIntStateOf(0) }
    var loading by remember(model) { mutableStateOf(true) }
    var failed by remember(model) { mutableStateOf(false) }
    LaunchedEffect(online) { if (online && failed) attempt++ }
    Box(modifier) {
        key(model, attempt) {
        AsyncImage(model, contentDescription, Modifier.matchParentSize(), contentScale = contentScale,
            onLoading = { loading = true; failed = false },
            onSuccess = { loading = false; failed = false },
            onError = { loading = false; failed = true })
        }
        if (loading) Skeleton(Modifier.matchParentSize())
        if (failed) Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
fun NetworkErrorState(onRetry: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().testTag("SCR-0603").padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(Icons.Outlined.WifiOff, null, Modifier.padding(24.dp).size(40.dp), tint = Sunset.Text3)
        }
        Spacer(Modifier.height(24.dp))
        Text(tr("You're offline"), fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(tr("Check Wi-Fi or mobile data - the river will still be here."),
            modifier = Modifier.padding(horizontal = 10.dp), fontSize = 13.sp, lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        ActionButton("Try again", {
            if (com.shorts.flowdrama.network.hasInternetConnection(context)) onRetry()
            else openInternetSettings(context)
        })
        TextButton(onClick = { openInternetSettings(context) }) { Text(tr("Open network settings")) }
    }
}

@Composable
fun EpisodeGridSkeleton() {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(5) { Skeleton(Modifier.weight(1f).height(48.dp)) }
            }
        }
    }
}

/** Player surfaces remain dark even when the browsing theme is light. */
@Composable
fun MediaNetworkErrorState(onRetry: () -> Unit) {
    FlowTheme(LocalLanguage.current) { NetworkErrorState(onRetry) }
}
