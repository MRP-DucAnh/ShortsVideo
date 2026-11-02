package com.shorts.flowdrama.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shorts.flowdrama.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val preferences: PreferencesRepository,
    private val reminders: ReminderPreferencesRepository,
) : ViewModel() {
    val selectedReminderWindows = reminders.selectedWindows
    private val mutableSaveError = MutableStateFlow(false)
    val saveError = mutableSaveError.asStateFlow()
    private val tastes = TasteSelectionPolicy()

    fun toggleMood(mood: TasteMood) = save {
        preferences.update { current ->
            val next =
                tastes.toggleMood(
                    mood,
                    TasteSelection(current.moodIds, current.tasteIds),
                )
            current.copy(moodIds = next.moodIds, tasteIds = next.tagIds)
        }
    }

    fun toggleGenre(tagId: String) = save {
        preferences.update { it.copy(tasteIds = tastes.toggleGenre(tagId, it.tasteIds)) }
    }

    fun setRemindersEnabled(enabled: Boolean) = save { reminders.setEnabled(enabled) }

    fun toggleReminder(window: ReminderWindow) = save { reminders.toggle(window) }

    private fun save(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                mutableSaveError.value = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableSaveError.value = true
            }
        }
    }
}
