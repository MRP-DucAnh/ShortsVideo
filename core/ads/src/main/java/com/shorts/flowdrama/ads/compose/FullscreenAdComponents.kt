package com.shorts.flowdrama.ads.compose

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shorts.flowdrama.ads.*
import com.shorts.flowdrama.ads.nativead.*

/** Preload once the SDK is initialized, and again if Remote Config changes its ID. */
@Composable
fun PreloadAdEffect(controller: AdsController, placement: String) {
    val config by controller.config.collectAsState()
    val ready by controller.ready.collectAsState()
    LaunchedEffect(controller, placement, config, ready) { if (ready) controller.preload(placement) }
}

/** Opt-in foreground hook. Caller supplies eligibility (for example, only on its loading screen). */
@Composable
fun AppOpenAdEffect(controller: AdsController, activity: Activity, placement: String, enabled: Boolean) {
    PreloadAdEffect(controller, placement)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val eligible by rememberUpdatedState(enabled)
    DisposableEffect(controller, activity, lifecycle, placement) {
        var returningFromFullscreenAd = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) returningFromFullscreenAd = controller.isFullscreenShowing.value
            if (event == Lifecycle.Event.ON_START) {
                if (eligible && !returningFromFullscreenAd) controller.showAppOpen(activity, placement)
                returningFromFullscreenAd = false
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

/** Callers provide localized copy that describes the reward; skip never grants a reward. */
@Composable
fun RewardedInterstitialPrompt(
    controller: AdsController, activity: Activity, placement: String,
    title: String, description: String, watchLabel: String, skipLabel: String,
    onReward: (AdReward) -> Unit, onComplete: (AdShowResult) -> Unit, onSkip: () -> Unit,
) {
    PreloadAdEffect(controller, placement)
    var submitted by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(title) }, text = { Text(description) },
        confirmButton = { TextButton(enabled = !submitted, onClick = {
            submitted = true
            controller.showFullscreenAd(activity, placement, onReward) {
                submitted = false
                onComplete(it)
            }
        }) { Text(watchLabel) } },
        dismissButton = { TextButton(onClick = onSkip) { Text(skipLabel) } },
    )
}

/** Full native templates are opt-in dialogs with an immediately available close action. */
@Composable
fun NativeAdDialog(state: NativeAdState, attribution: String, closeLabel: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().safeDrawingPadding().padding(12.dp),
            color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(
                state.placement?.style?.fullBackground ?: "#160F0A"))) {
            Column {
                TextButton(onClick = onDismiss) { Text(closeLabel) }
                NativeAdCard(state, attribution)
            }
        }
    }
}
