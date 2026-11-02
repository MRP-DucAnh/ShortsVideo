package com.shorts.flowdrama.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.shorts.flowdrama.domain.Preferences
import com.shorts.flowdrama.domain.PreferencesRepository
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.store by preferencesDataStore("flowdrama")

class DataStorePreferences(context: Context, scope: CoroutineScope) : PreferencesRepository {
    private val store = context.applicationContext.store
    private val mutex = Mutex()
    private val ready = CompletableDeferred<Unit>()
    private val state = MutableStateFlow(Preferences(language = deviceLanguage()))
    override val preferences = state.asStateFlow()

    init {
        scope.launch {
            store.data
                .catch { emit(emptyPreferences()) }
                .collect { p ->
                    state.value =
                        Preferences(
                            if (p[booleanPreferencesKey("device")] != false) deviceLanguage()
                            else p[stringPreferencesKey("language")] ?: deviceLanguage(),
                            p[booleanPreferencesKey("device")] ?: true,
                            p[booleanPreferencesKey("onboarded")] ?: false,
                            p[stringSetPreferencesKey("tastes")] ?: emptySet(),
                            p[booleanPreferencesKey("light")] ?: false,
                            p[booleanPreferencesKey("wifi")] ?: true,
                            p[floatPreferencesKey("speed")] ?: 1f,
                            p[stringPreferencesKey("quality")] ?: "Auto",
                            p[stringPreferencesKey("audio")],
                            p[stringSetPreferencesKey("moods")] ?: emptySet(),
                        )
                    ready.complete(Unit)
                }
        }
    }

    suspend fun awaitReady() {
        ready.await()
    }

    override suspend fun update(transform: (Preferences) -> Preferences) {
        ready.await()
        mutex.withLock {
            val next = transform(state.value)
            store.edit { p ->
                p[stringPreferencesKey("language")] = next.language
                p[booleanPreferencesKey("device")] = next.followDevice
                p[booleanPreferencesKey("onboarded")] = next.onboardingDone
                p[stringSetPreferencesKey("tastes")] = next.tasteIds
                p[stringSetPreferencesKey("moods")] = next.moodIds
                p[booleanPreferencesKey("light")] = next.lightTheme
                p[booleanPreferencesKey("wifi")] = next.wifiPreviewOnly
                p[floatPreferencesKey("speed")] = next.speed
                p[stringPreferencesKey("quality")] = next.quality
                next.audioLanguage?.let { p[stringPreferencesKey("audio")] = it }
                    ?: p.remove(stringPreferencesKey("audio"))
            }
            state.value = next
        }
    }

    companion object {
        fun deviceLanguage(): String =
            Locale.getDefault().language.takeIf {
                it in setOf("en", "hi", "ta", "id", "ar", "fr", "es", "pt", "tr", "de")
            } ?: "en"
    }
}
