package com.shorts.flowdrama.domain

import kotlinx.coroutines.flow.StateFlow

enum class TasteMood(val id: String, val tagIds: Set<String>) {
    Rage("rage", setOf("revenge", "rebirth", "urban", "underdog-triumph", "comeback")),
    Swoon(
        "swoon",
        setOf(
            "romance",
            "sweet-romance",
            "ceo",
            "contract-marriage",
            "marriage-first-love-later",
            "love",
        ),
    ),
    Cry("cry", setOf("family", "fate", "angsty-romance", "reunion", "redemption")),
    Thrill("thrill", setOf("suspense", "horror", "action", "plot-twist")),
}

data class TasteSelection(val moodIds: Set<String>, val tagIds: Set<String>)

/** One mood at a time; fine-tune genres are chosen independently by the user. */
class TasteSelectionPolicy {
    fun toggleMood(
        mood: TasteMood,
        current: TasteSelection,
    ): TasteSelection = TasteSelection(setOf(mood.id), current.tagIds)

    fun toggleGenre(id: String, current: Set<String>): Set<String> =
        if (id in current) current - id else current + id
}

/** Stable identifiers and local times are ready for a future reminder scheduler. */
enum class ReminderWindow(val id: String, val hour: Int, val minute: Int) {
    Morning("morning", 7, 0),
    Lunch("lunch", 12, 30),
    Night("night", 21, 0),
}

interface ReminderPreferencesRepository {
    val selectedWindows: StateFlow<Set<ReminderWindow>>
    val enabled: StateFlow<Boolean>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun toggle(window: ReminderWindow)
}
