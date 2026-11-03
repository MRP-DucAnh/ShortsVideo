package com.shorts.flowdrama

import com.shorts.flowdrama.ads.AdShowResult
import com.shorts.flowdrama.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class EpisodeUnlockCoordinatorTest {
    private class Prefs : PreferencesRepository {
        override val preferences = MutableStateFlow(Preferences())
        var writes = 0
        var fail = false
        override suspend fun update(transform: (Preferences) -> Preferences) {
            writes++
            if (fail) throw IllegalStateException("disk error")
            preferences.value = transform(preferences.value)
        }
    }
    private fun scenario(test: (Prefs, EpisodeUnlockCoordinator) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try { val prefs = Prefs(); test(prefs, EpisodeUnlockCoordinator(prefs, scope)) }
        finally { scope.cancel() }
    }

    @Test fun dismissingOrNoFillDoesNotUnlockEpisode() = scenario { prefs, unlock ->
        unlock.request("12692", 6)
        unlock.begin()
        unlock.complete(AdShowResult.Dismissed)
        assertEquals(EpisodeUnlockError.NotEarned, unlock.state.value.error)
        assertTrue(prefs.preferences.value.unlockedEpisodeIds.isEmpty())
        unlock.begin()
        unlock.complete(AdShowResult.NotReady)
        assertEquals(EpisodeUnlockError.Unavailable, unlock.state.value.error)
        assertEquals(0, prefs.writes)
    }

    @Test fun earnedRewardPersistsOnlyExactEpisodeAndContinuesAfterDismissal() = scenario { prefs, unlock ->
        unlock.request("12692", 6)
        val target = requireNotNull(unlock.begin())
        unlock.reward(target.copy(showId = "other"))
        assertEquals(0, prefs.writes)
        unlock.reward(target)
        unlock.reward(target)
        assertEquals(1, prefs.writes)
        assertEquals(setOf("12692:6"), prefs.preferences.value.unlockedEpisodeIds)
        assertNull(unlock.state.value.completed)
        unlock.complete(AdShowResult.Dismissed)
        assertEquals(target, unlock.state.value.completed)
        unlock.consumeCompletion()
        assertNull(unlock.state.value.completed)
    }

    @Test fun failedSaveCanRetryWithoutWatchingAnotherAd() = scenario { prefs, unlock ->
        unlock.request("12692", 6)
        val target = requireNotNull(unlock.begin())
        prefs.fail = true
        unlock.reward(target)
        unlock.complete(AdShowResult.Dismissed)
        assertEquals(EpisodeUnlockError.SaveFailed, unlock.state.value.error)
        prefs.fail = false
        assertNull(unlock.begin())
        assertEquals(target, unlock.state.value.completed)
        assertEquals(setOf("12692:6"), prefs.preferences.value.unlockedEpisodeIds)
    }

    @Test fun rewardWithoutAnAcceptedRequestIsIgnored() = scenario { prefs, unlock ->
        unlock.request("12692", 6)
        unlock.reward(EpisodeUnlockTarget("12692", 6))
        assertEquals(0, prefs.writes)
        unlock.dismiss()
        assertNull(unlock.state.value.target)
    }
}
