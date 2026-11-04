package com.shorts.flowdrama.playback

import android.content.Context

/** Video audio selection is independent of the application's display language. */
internal class PlaybackAudioPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "playback_audio",
        Context.MODE_PRIVATE,
    )

    var language: String?
        get() = preferences.getString("language", null)
        set(value) {
            preferences.edit().putString("language", value).apply()
        }
}
