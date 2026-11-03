package com.shorts.flowdrama.ads.compose

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.shorts.flowdrama.core.ads.R
import java.util.Locale

@Composable
fun adAttribution(language: String): String {
    val context = LocalContext.current
    val localized = remember(context, language) {
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(Locale.forLanguageTag(language)))
        context.createConfigurationContext(config)
    }
    return localized.getString(R.string.ads_attribution)
}
