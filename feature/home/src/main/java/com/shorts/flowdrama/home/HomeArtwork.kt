package com.shorts.flowdrama.home

import android.content.Context
import coil3.SingletonImageLoader
import coil3.size.Scale
import coil3.request.ImageRequest
import com.shorts.flowdrama.domain.Catalog

internal fun Catalog.homeHero() = shows.find { it.id == heroShowId } ?: shows.firstOrNull()

/** Use identical dimensions for preloading and display so the decoded image can be reused. */
internal fun homeHeroRequest(context: Context, url: String): ImageRequest {
    val metrics = context.resources.displayMetrics
    return ImageRequest.Builder(context)
        .data(url)
        .scale(Scale.FILL)
        .size((metrics.widthPixels - 32 * metrics.density).toInt().coerceAtLeast(1),
            (238 * metrics.density).toInt().coerceAtLeast(1))
        .build()
}

/** Warm the shared Coil cache while Splash/onboarding is visible; navigation never waits for it. */
suspend fun preloadHomeHero(context: Context, catalog: Catalog) {
    val hero = catalog.homeHero() ?: return
    SingletonImageLoader.get(context).execute(homeHeroRequest(context, hero.posterUrl))
}
