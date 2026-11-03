package com.shorts.flowdrama.ads

import com.shorts.flowdrama.ads.config.AdFormatPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

enum class AdLoadStatus { Idle, Loading, Loaded, Failed, Disabled }
enum class AdShowResult { Dismissed, Disabled, NotReady, Busy, RateLimited, ActivityUnavailable, Failed }
data class AdReward(val type: String, val amount: Int)

internal suspend fun <T> loadWithRetry(policy: AdFormatPolicy, load: suspend () -> T): T {
    var attempt = 0
    while (true) {
        try {
            return withTimeout(15_000) { load() }
        } catch (cancelled: CancellationException) {
            // A timeout is a failed request; screen disposal must cancel the whole retry loop.
            if (cancelled !is kotlinx.coroutines.TimeoutCancellationException) throw cancelled
            if (!policy.retryEnabled || attempt >= policy.maxRetries) throw cancelled
        } catch (error: Exception) {
            if (!policy.retryEnabled || attempt >= policy.maxRetries) throw error
        }
        delay(policy.retryDelayMillis(attempt++))
    }
}
