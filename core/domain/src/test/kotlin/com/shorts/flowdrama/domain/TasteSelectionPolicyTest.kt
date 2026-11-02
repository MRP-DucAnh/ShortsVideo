package com.shorts.flowdrama.domain

import org.junit.Assert.*
import org.junit.Test

class TasteSelectionPolicyTest {
    private val policy = TasteSelectionPolicy()

    @Test
    fun selectingMoodReplacesPreviousMoodWithoutChoosingGenres() {
        val first = policy.toggleMood(TasteMood.Rage, TasteSelection(emptySet(), emptySet()))
        assertEquals(TasteSelection(setOf("rage"), emptySet()), first)
        val next = policy.toggleMood(TasteMood.Swoon, first)
        assertEquals(TasteSelection(setOf("swoon"), emptySet()), next)
        assertEquals(next, policy.toggleMood(TasteMood.Swoon, next))
    }

    @Test
    fun switchingMoodPreservesAllManualGenres() {
        val genres = setOf("comedy", "ceo", "revenge", "urban", "plot-twist")
        val current = TasteSelection(setOf("rage", "cry"), genres)
        val next = policy.toggleMood(TasteMood.Thrill, current)
        assertEquals(setOf("thrill"), next.moodIds)
        assertEquals(genres, next.tagIds)
    }

    @Test
    fun fineTuneAllowsAnyNumberOfGenresAndDeselectingEach() {
        val ids = TasteMood.entries.flatMap { it.tagIds }.toSet()
        val selected = ids.fold(emptySet<String>()) { current, id -> policy.toggleGenre(id, current) }
        assertEquals(ids, selected)
        assertTrue(ids.fold(selected) { current, id -> policy.toggleGenre(id, current) }.isEmpty())
    }
}
