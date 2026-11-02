package com.shorts.flowdrama.data

import android.content.Context
import com.shorts.flowdrama.domain.*
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request

/** Local-first metadata keeps startup independent of network, ads and Firebase. */
class DefaultCatalogRepository(context: Context, private val scope: CoroutineScope) :
    CatalogRepository {
    private val app = context.applicationContext
    private val parser = JsonCatalog()
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    private val mutex = Mutex()
    private var catalog: Catalog? = null
    private val episodeCache = mutableMapOf<String, List<Episode>>()
    private val requested = mutableSetOf<String>()

    override suspend fun catalog(refresh: Boolean): Catalog =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (!refresh)
                    catalog?.let {
                        return@withLock it
                    }
                val result = load("catalog.json", parser::catalog)
                catalog = result
                result
            }
        }

    override suspend fun episodes(showId: String): List<Episode> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                require(showId.matches(Regex("[0-9]+")))
                episodeCache.getOrPut(showId) { load("shows/$showId.json", parser::episodes) }
            }
        }

    private fun <T> load(path: String, parse: (String) -> T): T {
        val file = File(app.filesDir, "catalog/$path")
        val cached = if (file.exists()) runCatching { parse(file.readText()) }.getOrNull() else null
        val local =
            cached
                ?: runCatching {
                    parse(app.assets.open("fixtures/$path").bufferedReader().use { it.readText() })
                }
                    .getOrNull()
        // The task fixtures match the live API. Refresh persistent metadata in the background;
        // a refreshed snapshot is used on the next cold launch, without changing a grid
        // mid-session.
        if (
            (!file.exists() ||
                System.currentTimeMillis() - file.lastModified() >= TimeUnit.HOURS.toMillis(6)) &&
                local != null &&
                requested.add(path)
        ) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    client
                        .newCall(
                            Request.Builder()
                                .url("https://flowdrama.luonluon.com/flowdrama/api/$path")
                                .build()
                        )
                        .execute()
                        .use { response ->
                            check(response.isSuccessful) { "HTTP ${response.code}" }
                            val raw = checkNotNull(response.body).string()
                            parse(raw)
                            file.parentFile?.mkdirs()
                            val temporary = File(file.parentFile, file.name + ".tmp")
                            temporary.writeText(raw)
                            if (!temporary.renameTo(file)) {
                                file.writeText(raw)
                                temporary.delete()
                            }
                        }
                }
            }
        }
        return local
            ?: client
                .newCall(
                    Request.Builder()
                        .url("https://flowdrama.luonluon.com/flowdrama/api/$path")
                        .build()
                )
                .execute()
                .use { response ->
                    check(response.isSuccessful) { "HTTP ${response.code}" }
                    val raw = checkNotNull(response.body).string()
                    parse(raw).also {
                        file.parentFile?.mkdirs()
                        file.writeText(raw)
                    }
                }
    }
}
