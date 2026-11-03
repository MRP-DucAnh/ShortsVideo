package com.shorts.flowdrama.home

import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.shorts.flowdrama.designsystem.LocalLanguage
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.feature.home.R
import java.util.Locale

@Composable
internal fun homeString(@StringRes id: Int, vararg args: Any): String {
    val context = LocalContext.current
    val language = LocalLanguage.current
    val localized = remember(context, language) {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList(Locale.forLanguageTag(language)))
        context.createConfigurationContext(configuration)
    }
    return localized.getString(id, *args)
}

internal fun Daypart.titleResource() = when (this) {
    Daypart.Morning -> R.string.home_morning
    Daypart.Lunch -> R.string.home_lunch
    Daypart.Night -> R.string.home_night
}

internal fun Daypart.subtitleResource() = when (this) {
    Daypart.Morning -> R.string.home_morning_detail
    Daypart.Lunch -> R.string.home_lunch_detail
    Daypart.Night -> R.string.home_night_detail
}

internal fun Daypart.badgeResource() = when (this) {
    Daypart.Morning -> R.string.home_morning_pick
    Daypart.Lunch -> R.string.home_lunch_pick
    Daypart.Night -> R.string.home_night_pick
}

@Composable
internal fun heroMetadata(show: Show, tags: List<Tag>): String {
    val status = when (show.status.lowercase(Locale.ROOT)) {
        "completed" -> homeString(R.string.home_completed)
        "ongoing" -> homeString(R.string.home_ongoing)
        else -> show.status
    }
    val language = LocalLanguage.current
    val genres = show.tagIds.take(2).mapNotNull { id ->
        tags.find { it.id == id }?.let { LocalizedTextResolver().resolve(it.name, language) }
    }
    return (listOf(homeString(R.string.home_episode_count, show.episodeCount), status) + genres)
        .filter { it.isNotBlank() }.joinToString(" · ")
}
