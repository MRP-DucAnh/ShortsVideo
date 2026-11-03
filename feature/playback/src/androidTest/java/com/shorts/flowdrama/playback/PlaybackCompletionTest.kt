package com.shorts.flowdrama.playback

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shorts.flowdrama.domain.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackCompletionTest {
    @Test fun progressAt99PercentIsNotWatchedButCompletionIsPersisted() = runBlocking {
        val writes = CopyOnWriteArrayList<Progress>()
        val library = object : LibraryRepository {
            override val savedIds = MutableStateFlow<Set<String>>(emptySet())
            override val history = MutableStateFlow<List<Progress>>(emptyList())
            override val episodeProgress = MutableStateFlow<List<Progress>?>(emptyList())
            override val likedShowIds = MutableStateFlow<Set<String>>(emptySet())
            override val likedEpisodes = MutableStateFlow<Set<EpisodeKey>>(emptySet())
            override suspend fun toggleSaved(showId: String) = Unit
            override suspend fun toggleShowLike(showId: String) = Unit
            override suspend fun toggleEpisodeLike(showId: String, episodeNumber: Int) = Unit
            override suspend fun saveProgress(progress: Progress) { writes.add(progress) }
            override suspend fun clearHistory() = Unit
            override suspend fun removeHistory(showId: String) = Unit
            override suspend fun removeEpisodeHistory(showId: String, episodeNumber: Int) = Unit
        }
        val prefs = object : PreferencesRepository {
            override val preferences = MutableStateFlow(Preferences())
            override suspend fun update(transform: (Preferences) -> Preferences) = Unit
        }
        val episode = Episode(1, true, 100, 0, emptyMap())
        val catalog = object : CatalogRepository {
            override suspend fun catalog(refresh: Boolean): Catalog = error("unused")
            override suspend fun episodes(showId: String) = listOf(episode)
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            val vm = withContext(Dispatchers.Main) {
                PlaybackViewModel(catalog, library, scope, prefs).also { it.load("show", 1) }
            }
            withTimeout(5000) { vm.state.first { !it.loading && it.current != null } }
            withContext(Dispatchers.Main) { vm.persist(1, 99000, 100000) }
            withTimeout(5000) { while (writes.isEmpty()) delay(10) }
            assertFalse(writes.last().watched)
            withContext(Dispatchers.Main) { vm.completeEpisode(1, 100000) }
            withTimeout(5000) { while (writes.none { it.watched }) delay(10) }
            assertEquals(100000L, writes.last { it.watched }.positionMs)
            assertTrue(writes.last { it.watched }.finished)
        } finally { scope.cancel() }
    }
}
