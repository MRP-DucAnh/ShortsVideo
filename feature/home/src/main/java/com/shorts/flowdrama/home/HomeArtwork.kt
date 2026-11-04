package com.shorts.flowdrama.home

import android.content.Context
import coil3.SingletonImageLoader
import coil3.size.Scale
import coil3.request.ImageRequest
import com.shorts.flowdrama.domain.Catalog
import com.shorts.flowdrama.domain.DaypartResolver
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Rotate through the catalog once per local daypart; recomposition keeps the same pick. */
internal fun Catalog.homeHero(now: Long = System.currentTimeMillis()): com.shorts.flowdrama.domain.Show? {
    val candidates = shows.distinctBy { it.id }.sortedWith(
        compareBy<com.shorts.flowdrama.domain.Show> { it.id != heroShowId }.thenBy { it.id }
    )
    if (candidates.isEmpty()) return null
    val calendar = Calendar.getInstance().apply { timeInMillis = now }
    val localDay = TimeUnit.MILLISECONDS.toDays(now + calendar.timeZone.getOffset(now))
    val daypart = DaypartResolver().resolve(calendar.get(Calendar.HOUR_OF_DAY))
    val slot = localDay * 3 + daypart.ordinal
    return candidates[Math.floorMod(slot, candidates.size.toLong()).toInt()]
}

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
