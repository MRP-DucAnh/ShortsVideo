package com.shorts.flowdrama

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.decode.DataSource
import coil3.size.Scale
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.shorts.flowdrama.data.JsonCatalog
import com.shorts.flowdrama.home.preloadHomeHero
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeArtworkTest {
    @Test
    fun bundledHeroLoadsWithoutCacheAndPreloadWarmsSharedMemoryCache() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = JsonCatalog().catalog(
            context.assets.open("fixtures/catalog.json").bufferedReader().use { it.readText() }
        )
        val hero = catalog.shows.first { it.id == catalog.heroShowId }
        val metrics = context.resources.displayMetrics
        val request = ImageRequest.Builder(context)
            .data(hero.posterUrl)
            .scale(Scale.FILL)
            .size((metrics.widthPixels - 32 * metrics.density).toInt(), (238 * metrics.density).toInt())
            .build()
        // A dedicated loader with both caches disabled reproduces a genuine first image request.
        val coldLoader = ImageLoader.Builder(context).memoryCache(null).diskCache(null).build()
        try {
            val coldResult = coldLoader.execute(request)
            assertTrue("Bundled hero must load on first launch: $coldResult", coldResult is SuccessResult)
        } finally {
            coldLoader.shutdown()
        }
        preloadHomeHero(context, catalog)
        val warmResult = SingletonImageLoader.get(context).execute(request)
        assertTrue(warmResult is SuccessResult)
        assertEquals(DataSource.MEMORY_CACHE, (warmResult as SuccessResult).dataSource)
    }
}
