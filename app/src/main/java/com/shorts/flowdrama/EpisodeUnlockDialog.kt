package com.shorts.flowdrama

import android.app.Activity
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorts.flowdrama.ads.*
import com.shorts.flowdrama.ads.compose.PreloadAdEffect
import com.shorts.flowdrama.ads.config.AdFormat
import com.shorts.flowdrama.designsystem.LocalLanguage
import java.util.Locale

@Composable
internal fun EpisodeUnlockDialog(
    ads: AdsController, coordinator: EpisodeUnlockCoordinator, state: EpisodeUnlockState,
    activity: Activity, showName: String,
) {
    val target = state.target ?: return
    val config by ads.config.collectAsStateWithLifecycle()
    val statuses by ads.fullscreenStatus.collectAsStateWithLifecycle()
    PreloadAdEffect(ads, target.placement)
    val format = config.placements[target.placement]?.format
    val rewarded = format == AdFormat.Rewarded || format == AdFormat.RewardedInterstitial
    val status = statuses[target.placement] ?: AdLoadStatus.Idle
    val savingRetry = state.error == EpisodeUnlockError.SaveFailed
    val loading = status == AdLoadStatus.Loading && !savingRetry
    val ready = rewarded && ads.allowed(target.placement) && status == AdLoadStatus.Loaded
    val message = when {
        state.busy -> R.string.ad_unlock_saving
        savingRetry -> R.string.ad_unlock_save_failed
        state.error == EpisodeUnlockError.NotEarned -> R.string.ad_unlock_not_earned
        state.error == EpisodeUnlockError.Unavailable || !rewarded || !ads.allowed(target.placement) || status == AdLoadStatus.Failed -> R.string.ad_unlock_unavailable
        loading -> R.string.ad_unlock_loading
        else -> null
    }
    AlertDialog(
        modifier = Modifier.testTag("ads:episode-unlock"),
        onDismissRequest = coordinator::dismiss,
        title = { Text(appString(R.string.ad_unlock_title, target.number)) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(appString(R.string.ad_unlock_description, target.number, showName))
                if (message != null) Text(appString(message))
            }
        },
        confirmButton = {
            TextButton(enabled = !state.busy && !loading, onClick = {
                if (savingRetry) coordinator.begin()
                else if (!ready) ads.preload(target.placement)
                else {
                    val currentFormat = ads.config.value.placements[target.placement]?.format
                    val accepted = coordinator.begin() ?: return@TextButton
                    if (currentFormat != AdFormat.Rewarded && currentFormat != AdFormat.RewardedInterstitial) {
                        coordinator.complete(AdShowResult.Disabled)
                    } else ads.showFullscreenAd(activity, target.placement,
                        onReward = { coordinator.reward(accepted) }, onComplete = coordinator::complete)
                }
            }) {
                Text(appString(if (ready && !savingRetry) R.string.ad_unlock_watch else R.string.ad_unlock_retry))
            }
        },
        dismissButton = {
            TextButton(enabled = !state.busy, onClick = coordinator::dismiss) { Text(appString(R.string.ad_unlock_not_now)) }
        },
    )
}

@Composable
internal fun appString(@StringRes id: Int, vararg args: Any): String {
    val context = LocalContext.current
    val language = LocalLanguage.current
    val localized = remember(context, language) {
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(Locale.forLanguageTag(language)))
        context.createConfigurationContext(config)
    }
    return localized.getString(id, *args)
}
