package com.shorts.flowdrama.onboarding

import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.shorts.flowdrama.designsystem.LocalLanguage
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.feature.onboarding.R
import java.util.Locale

/** The app locale is state-driven; resolve Android resources without mutating the Activity. */
@Composable
internal fun onboardingString(@StringRes id: Int, vararg args: Any): String {
    val context = LocalContext.current
    val language = LocalLanguage.current
    val localized =
        remember(context, language) {
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocales(LocaleList(Locale.forLanguageTag(language)))
            context.createConfigurationContext(configuration)
        }
    return localized.getString(id, *args)
}

@StringRes
internal fun TasteMood.titleResource(): Int =
    when (this) {
        TasteMood.Rage -> R.string.ob_rage
        TasteMood.Swoon -> R.string.ob_swoon
        TasteMood.Cry -> R.string.ob_cry
        TasteMood.Thrill -> R.string.ob_thrill
    }

@StringRes
internal fun TasteMood.detailResource(): Int =
    when (this) {
        TasteMood.Rage -> R.string.ob_rage_detail
        TasteMood.Swoon -> R.string.ob_swoon_detail
        TasteMood.Cry -> R.string.ob_cry_detail
        TasteMood.Thrill -> R.string.ob_thrill_detail
    }

internal val TasteMood.emoji: String
    get() =
        when (this) {
            TasteMood.Rage -> "🔥"
            TasteMood.Swoon -> "😍"
            TasteMood.Cry -> "😭"
            TasteMood.Thrill -> "😱"
        }

@Composable
internal fun genreTitle(tag: Tag): String {
    val resource =
        when (tag.id) {
            "fantasy" -> R.string.ob_genre_fantasy
            "ceo" -> R.string.ob_genre_ceo
            "sweet-romance" -> R.string.ob_genre_sweet_romance
            "comedy" -> R.string.ob_genre_comedy
            "plot-twist" -> R.string.ob_genre_plot_twist
            "urban" -> R.string.ob_genre_urban
            "contract-marriage" -> R.string.ob_genre_contract_marriage
            "marriage-first-love-later" -> R.string.ob_genre_marriage_first_love_later
            "angsty-romance" -> R.string.ob_genre_angsty_romance
            "reunion" -> R.string.ob_genre_reunion
            "love" -> R.string.ob_genre_love
            "comeback" -> R.string.ob_genre_comeback
            "revenge" -> R.string.ob_genre_revenge
            "underdog-triumph" -> R.string.ob_genre_underdog_triumph
            "redemption" -> R.string.ob_genre_redemption
            else -> null
        }
    return resource?.let { onboardingString(it) }
        ?: LocalizedTextResolver().resolve(tag.name, LocalLanguage.current)
}
