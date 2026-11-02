package com.shorts.flowdrama.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/** A bounded media cache is shared by playback and next-page prefetch. */
internal object MediaCache {
    @Volatile private var instance: SimpleCache? = null

    private fun cache(context: Context): SimpleCache =
        instance
            ?: synchronized(this) {
                instance
                    ?: SimpleCache(
                            File(context.applicationContext.cacheDir, "video"),
                            LeastRecentlyUsedCacheEvictor(64L * 1024 * 1024),
                            StandaloneDatabaseProvider(context.applicationContext),
                        )
                        .also { instance = it }
            }

    fun factory(context: Context): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(cache(context))
            .setUpstreamDataSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setConnectTimeoutMs(5000)
                    .setReadTimeoutMs(8000)
                    .setUserAgent("FlowDrama/1.0")
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}
