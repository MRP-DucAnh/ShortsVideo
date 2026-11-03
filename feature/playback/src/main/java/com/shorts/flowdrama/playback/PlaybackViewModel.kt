package com.shorts.flowdrama.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorts.flowdrama.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class PlaybackState(
    val episodes: List<Episode> = emptyList(),
    val current: Episode? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
)

class PlaybackViewModel(
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
    private val persistenceScope: CoroutineScope,
    private val preferences: PreferencesRepository,
) : ViewModel() {
    private val mutable = MutableStateFlow(PlaybackState())
    val state = combine(mutable, preferences.preferences) { raw, prefs ->
        raw.copy(
            episodes = raw.episodes.map { it.withLocalAccess(showId, prefs.unlockedEpisodeIds) },
            current = raw.current?.withLocalAccess(showId, prefs.unlockedEpisodeIds),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackState())
    private var showId = ""
    private var job: Job? = null
    private var lastPersistedAt = 0L
    private var lastPersistedEpisode = -1
    private var lastPosition = -1L

    fun load(id: String, number: Int) {
        if (id == showId && state.value.current?.number == number) return
        showId = id
        job?.cancel()
        mutable.value = PlaybackState()
        job = viewModelScope.launch {
            try {
                val episodes = catalog.episodes(id)
                val selected =
                    episodes.map { it.withLocalAccess(id, preferences.preferences.value.unlockedEpisodeIds) }
                        .find { it.number == number && it.isFree }
                        ?: episodes.firstOrNull { it.isFree }
                mutable.value = PlaybackState(episodes, selected, loading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.value = PlaybackState(loading = false, error = true)
            }
        }
    }

    fun select(episode: Episode) {
        if (episode.isFree) mutable.value = state.value.copy(current = episode)
    }

    fun checkpoint(number: Int, position: Long, duration: Long) {
        lastPersistedAt = 0
        persist(number, position, duration)
    }

    fun persist(number: Int, position: Long, duration: Long) {
        val id = showId
        if (position < 0 || id.isEmpty()) return
        val now = System.currentTimeMillis()
        if (number == lastPersistedEpisode && position == lastPosition) return
        if (
            number == lastPersistedEpisode &&
                now - lastPersistedAt < 5000 &&
                duration > 0 &&
                position < duration * .95
        )
            return
        lastPersistedAt = now
        lastPersistedEpisode = number
        lastPosition = position
        persistenceScope.launch {
            library.saveProgress(
                Progress(
                    id,
                    number,
                    position,
                    duration,
                    System.currentTimeMillis(),
                    state.value.episodes.lastOrNull()?.number == number,
                )
            )
        }
    }
}
