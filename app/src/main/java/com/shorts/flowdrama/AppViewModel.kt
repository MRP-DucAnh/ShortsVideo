package com.shorts.flowdrama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorts.flowdrama.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AppState(
    val loading: Boolean = true,
    val catalog: Catalog? = null,
    val error: Boolean = false,
    val ready: Boolean = false,
)

class AppViewModel(private val container: AppContainer) : ViewModel() {
    val episodeUnlock = EpisodeUnlockCoordinator(container.preferences, viewModelScope)
    private val mutable = MutableStateFlow(AppState())
    val state = mutable.asStateFlow()
    val preferences = container.preferences.preferences
    val saved = container.library.savedIds
    val history = container.library.history
    private val messageFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = messageFlow.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            mutable.value = mutable.value.copy(loading = true, error = false)
            try {
                container.preferences.awaitReady()
                val catalog = container.catalog.catalog()
                mutable.value = AppState(false, catalog, false, true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutable.value = AppState(loading = false, error = true, ready = true)
            }
        }
    }

    fun update(transform: (Preferences) -> Preferences) {
        viewModelScope.launch {
            runCatching { container.preferences.update(transform) }
                .onFailure { messageFlow.tryEmit("Unable to save settings") }
        }
    }

    fun save(id: String) {
        viewModelScope.launch {
            val wasSaved = id in saved.value
            runCatching { container.library.toggleSaved(id) }
                .onSuccess {
                    messageFlow.emit(
                        if (wasSaved) "Removed from your Library" else "Saved to your Library"
                    )
                }
                .onFailure { messageFlow.tryEmit("Unable to save. Try again.") }
        }
    }

    fun removeHistory(id: String) {
        viewModelScope.launch { container.library.removeHistory(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { container.library.clearHistory() }
    }
}
