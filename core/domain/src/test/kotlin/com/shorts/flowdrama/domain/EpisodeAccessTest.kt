package com.shorts.flowdrama.domain

import org.junit.Assert.*
import org.junit.Test

class EpisodeAccessTest {
    @Test fun unlockIsScopedToShowAndEpisode() {
        val locked = Episode(6, false, 100, 0, emptyMap())
        val unlocked = setOf(episodeAccessKey("12692", 6))
        assertTrue(locked.withLocalAccess("12692", unlocked).isFree)
        assertFalse(locked.withLocalAccess("13077", unlocked).isFree)
        assertFalse(locked.copy(number = 7).withLocalAccess("12692", unlocked).isFree)
        assertFalse(locked.isFree)
    }
}
