package com.shorts.flowdrama

import com.shorts.flowdrama.ads.AdShowResult
import com.shorts.flowdrama.ads.config.AdPlacements
import com.shorts.flowdrama.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EpisodeUnlockTarget(val showId: String, val number: Int, val placement: String = AdPlacements.DetailEpisodeUnlockRewarded)
enum class EpisodeUnlockError { Unavailable, NotEarned, SaveFailed }
data class EpisodeUnlockState(
    val target: EpisodeUnlockTarget? = null,
    val busy: Boolean = false,
    val error: EpisodeUnlockError? = null,
    val completed: EpisodeUnlockTarget? = null,
)

/** Ad dismissal never grants access. Persist the exact earned episode, then continue after dismissal. */
class EpisodeUnlockCoordinator(private val preferences: PreferencesRepository, private val scope: CoroutineScope) {
    private val mutable = MutableStateFlow(EpisodeUnlockState())
    val state = mutable.asStateFlow()
    private var saving: Deferred<Boolean>? = null
    private var earned = false
    private var completing = false

    fun request(showId: String, number: Int, placement: String = AdPlacements.DetailEpisodeUnlockRewarded) {
        if (mutable.value.busy || mutable.value.completed != null) return
        earned = false
        completing = false
        saving = null
        mutable.value = EpisodeUnlockState(target = EpisodeUnlockTarget(showId, number, placement))
    }

    fun dismiss() { if (!mutable.value.busy) mutable.value = EpisodeUnlockState() }

    fun begin(): EpisodeUnlockTarget? {
        val state = mutable.value
        if (state.busy || state.target == null) return null
        if (earned && state.error == EpisodeUnlockError.SaveFailed) {
            mutable.value = state.copy(busy = true, error = null)
            saving = persist(state.target)
            complete(AdShowResult.Dismissed)
            return null
        }
        earned = false
        completing = false
        saving = null
        mutable.value = state.copy(busy = true, error = null)
        return state.target
    }

    fun reward(target: EpisodeUnlockTarget) {
        if (!mutable.value.busy || mutable.value.target != target || earned) return
        earned = true
        saving = persist(target)
    }

    fun complete(result: AdShowResult) {
        val target = mutable.value.target ?: return
        if (!mutable.value.busy || completing) return
        completing = true
        scope.launch {
            val granted = saving?.await() == true
            completing = false
            mutable.value = if (granted) EpisodeUnlockState(completed = target)
            else mutable.value.copy(busy = false, error = when {
                earned -> EpisodeUnlockError.SaveFailed
                result == AdShowResult.Dismissed -> EpisodeUnlockError.NotEarned
                else -> EpisodeUnlockError.Unavailable
            })
        }
    }

    fun consumeCompletion() { mutable.value = EpisodeUnlockState() }

    private fun persist(target: EpisodeUnlockTarget) = scope.async {
        try {
            preferences.update { it.copy(unlockedEpisodeIds = it.unlockedEpisodeIds + episodeAccessKey(target.showId, target.number)) }
            true
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { false }
    }
}
