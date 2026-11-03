package com.shorts.flowdrama.ads.fullscreen

import com.shorts.flowdrama.ads.config.AdFormat
import com.shorts.flowdrama.ads.config.AdFormatPolicy

/** Monotonic time is injected so frequency caps can be tested without Android or sleeps. */
internal class AdDisplayLimiter(private val now: () -> Long) {
    private data class Window(var start: Long, var count: Int = 0, var last: Long? = null)
    private val windows = mutableMapOf<AdFormat, Window>()

    fun allows(format: AdFormat, policy: AdFormatPolicy, ignoreInterval: Boolean): Boolean {
        val time = now()
        val window = windows.getOrPut(format) { Window(time) }
        if (time - window.start >= policy.sessionSeconds * 1000) {
            window.start = time
            window.count = 0
        }
        return window.count < policy.adsPerSession && (ignoreInterval || window.last == null ||
            time - requireNotNull(window.last) >= policy.intervalSeconds * 1000)
    }

    fun record(format: AdFormat) {
        val time = now()
        windows.getOrPut(format) { Window(time) }.apply { count++; last = time }
    }
}
