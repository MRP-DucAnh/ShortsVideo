package com.shorts.flowdrama.data

import android.content.Context
import com.shorts.flowdrama.domain.ReminderPreferencesRepository
import com.shorts.flowdrama.domain.ReminderWindow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SharedPreferencesReminders(context: Context, scope: CoroutineScope) :
    ReminderPreferencesRepository {
    private val storage =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val ready = CompletableDeferred<Unit>()
    private val selection = MutableStateFlow<Set<ReminderWindow>>(emptySet())
    override val selectedWindows = selection.asStateFlow()
    private val enabledState = MutableStateFlow(false)
    override val enabled = enabledState.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            val stored = storage.getStringSet(WINDOWS_KEY, emptySet()).orEmpty()
            selection.value = ReminderWindow.entries.filter { it.id in stored }.toSet()
            enabledState.value =
                storage.getBoolean(ENABLED_KEY, false)
            ready.complete(Unit)
        }
    }

    override suspend fun setEnabled(enabled: Boolean) =
        withContext(Dispatchers.IO) {
            ready.await()
            mutex.withLock {
                val next = enabled
                check(storage.edit().putBoolean(ENABLED_KEY, next).commit())
                enabledState.value = next
            }
        }

    override suspend fun toggle(window: ReminderWindow) =
        withContext(Dispatchers.IO) {
            ready.await()
            mutex.withLock {
                val current = selection.value
                val next = if (window in current) current - window else current + window
                check(
                    storage
                        .edit()
                        .putStringSet(WINDOWS_KEY, next.map { it.id }.toSet())
                        .commit()
                )
                selection.value = next
            }
        }

    companion object {
        const val FILE_NAME = "onboarding_reminders"
        const val ENABLED_KEY = "reminders_enabled"
        const val WINDOWS_KEY = "selected_window_ids"
    }
}
