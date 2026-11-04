package com.shorts.flowdrama.designsystem

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.shorts.flowdrama.core.designsystem.R
import java.util.Locale

private val sharedStringIds = mapOf(
    "Airing" to R.string.ui_airing_6b3a6f,
    "All" to R.string.ui_all_6a7208,
    "Also flowing tonight" to R.string.ui_also_flowing_tonight_7bed53,
    "App language" to R.string.ui_app_language_01e178,
    "Appearance" to R.string.ui_appearance_41def7,
    "Audio language" to R.string.ui_audio_language_3bb89c,
    "Auto" to R.string.ui_auto_c614ba,
    "Back" to R.string.ui_back_b52b36,
    "Browse" to R.string.ui_browse_2f3b5c,
    "Cancel" to R.string.ui_cancel_77dfd2,
    "Check Wi-Fi or mobile data - the river will still be here." to R.string.ui_check_wi_fi_or_mobile_data_the_river_will_still_be_here_c36b27,
    "Choose manually" to R.string.ui_choose_manually_dc4598,
    "Choose your language" to R.string.ui_choose_your_language_1ee9cc,
    "Clear" to R.string.ui_clear_719ea3,
    "Clear history" to R.string.ui_clear_history_53b515,
    "Close" to R.string.ui_close_bbfa77,
    "Completed" to R.string.ui_completed_1798b3,
    "Continue" to R.string.ui_continue_2e0262,
    "Continue watching" to R.string.ui_continue_watching_8fca30,
    "Cry" to R.string.ui_cry_1ce449,
    "Dark" to R.string.ui_dark_ae1ef0,
    "Episode locked" to R.string.ui_episode_locked_ebeaeb,
    "Episodes" to R.string.ui_episodes_49d641,
    "Find your next flow" to R.string.ui_find_your_next_flow_92b01e,
    "Finished" to R.string.ui_finished_355bcc,
    "Home" to R.string.ui_home_70f8bb,
    "How should tonight feel?" to R.string.ui_how_should_tonight_feel_647bc1,
    "Language" to R.string.ui_language_89b86a,
    "Legal policy is not published for this build. Viewing history and preferences are stored anonymously on this device. This app uses Firebase Remote Config and Google test ads." to R.string.ui_legal_policy_is_not_published_for_this_build_viewing_hi_43cef6,
    "Library" to R.string.ui_library_b8100f,
    "Light" to R.string.ui_light_a36ef8,
    "Like" to R.string.ui_like_c7e02c,
    "Loading…" to R.string.ui_loading_33ce41,
    "Lunch splash" to R.string.ui_lunch_splash_ea7e10,
    "Match device language" to R.string.ui_match_device_language_cc432b,
    "Matches" to R.string.ui_matches_ee2dbd,
    "Morning sip" to R.string.ui_morning_sip_ea11e6,
    "New" to R.string.ui_new_6403f2,
    "New episode reminders are managed in Android notification settings." to R.string.ui_new_episode_reminders_are_managed_in_android_notificati_e05216,
    "Night flow" to R.string.ui_night_flow_dc926c,
    "No results" to R.string.ui_no_results_b993b0,
    "No watch history yet" to R.string.ui_no_watch_history_yet_bf4bcd,
    "Not now" to R.string.ui_not_now_e45714,
    "Notifications" to R.string.ui_notifications_753a22,
    "OK" to R.string.ui_ok_9ce3bd,
    "Open network settings" to R.string.ui_open_network_settings_be306d,
    "Open-source licenses" to R.string.ui_open_source_licenses_30dced,
    "Open-source software: Kotlin, AndroidX / Jetpack Compose, Media3, OkHttp and Coil (Apache 2.0). Firebase and Google Mobile Ads are covered by Google SDK terms. License details: docs/licenses.md." to R.string.ui_open_source_software_kotlin_androidx_jetpack_compose_me_36887f,
    "Pause" to R.string.ui_pause_781961,
    "Play" to R.string.ui_play_5d12bd,
    "Play Ep 1" to R.string.ui_play_ep_1_2c64c8,
    "Playback options" to R.string.ui_playback_options_75eab2,
    "Please try again." to R.string.ui_please_try_again_83a6fd,
    "Policies" to R.string.ui_policies_8d6118,
    "Preview on Wi-Fi only" to R.string.ui_preview_on_wi_fi_only_d9f970,
    "Privacy Policy" to R.string.ui_privacy_policy_9db108,
    "Quality" to R.string.ui_quality_a60f2c,
    "Rage" to R.string.ui_rage_513564,
    "Recently watched" to R.string.ui_recently_watched_de23e7,
    "Reels" to R.string.ui_reels_61e340,
    "Remove" to R.string.ui_remove_e96390,
    "Removed from your Library" to R.string.ui_removed_from_your_library_cc92bc,
    "Resume" to R.string.ui_resume_b3bd0b,
    "Save" to R.string.ui_save_efc007,
    "Saved" to R.string.ui_saved_c0ae8f,
    "Saved to your Library" to R.string.ui_saved_to_your_library_4c857d,
    "Search" to R.string.ui_search_bce064,
    "Search dramas" to R.string.ui_search_dramas_3d2a80,
    "See all" to R.string.ui_see_all_2941c5,
    "Settings" to R.string.ui_settings_c7f73b,
    "Short episodes. Big feelings. Your next story starts here." to R.string.ui_short_episodes_big_feelings_your_next_story_starts_here_e8c4a8,
    "Skip" to R.string.ui_skip_3da474,
    "Slow network — playing at 480p or lower" to R.string.ui_slow_network_playing_at_480p_or_lower_0cf6cf,
    "Slow network — trying a lower quality to keep playing" to R.string.ui_slow_network_trying_a_lower_quality_to_keep_playing_2308bd,
    "Speed" to R.string.ui_speed_2d2cb0,
    "Start a drama and pick up where you left off." to R.string.ui_start_a_drama_and_pick_up_where_you_left_off_5a3fb2,
    "Status" to R.string.ui_status_bae7d5,
    "Step into the flow" to R.string.ui_step_into_the_flow_1ec54f,
    "Switch back to HD" to R.string.ui_switch_back_to_hd_acd18e,
    "Swoon" to R.string.ui_swoon_5abd67,
    "Tap a poster · preview changes above" to R.string.ui_tap_a_poster_preview_changes_above_05de87,
    "Terms of Use" to R.string.ui_terms_of_use_977384,
    "This episode is currently locked. Unlocking is not available in this version." to R.string.ui_this_episode_is_currently_locked_unlocking_is_not_avail_f7d12a,
    "Thrill" to R.string.ui_thrill_f0b792,
    "Trending searches" to R.string.ui_trending_searches_525b27,
    "Try again" to R.string.ui_try_again_042c86,
    "Try another title or genre." to R.string.ui_try_another_title_or_genre_bda53d,
    "Turn on notifications" to R.string.ui_turn_on_notifications_5ab087,
    "Unable to load episodes" to R.string.ui_unable_to_load_episodes_ee00cf,
    "Unable to load your shows" to R.string.ui_unable_to_load_your_shows_ae4e4e,
    "Unable to save settings" to R.string.ui_unable_to_save_settings_04f7cc,
    "Unable to save. Try again." to R.string.ui_unable_to_save_try_again_cbb31c,
    "Video unavailable" to R.string.ui_video_unavailable_d0baed,
    "Video unavailable. Try again." to R.string.ui_video_unavailable_try_again_a7e8ca,
    "Views" to R.string.ui_views_24be61,
    "Watch a whole world unfold" to R.string.ui_watch_a_whole_world_unfold_f6d6b7,
    "Watch all %1\$d episodes" to R.string.ui_watch_all_1_d_episodes_1ffd5e,
    "Watch history" to R.string.ui_watch_history_2a8498,
    "Watching" to R.string.ui_watching_96d9e0,
    "You're offline" to R.string.ui_you_re_offline_f4d9ce,
    "Your flow runs three times a day" to R.string.ui_your_flow_runs_three_times_a_day_da00f1,
    "Your watchlist is empty" to R.string.ui_your_watchlist_is_empty_97f5da,
    "eps" to R.string.ui_eps_85962f,
)

/** Resolves UI copy in the app's selected language, independently of the device locale. */
fun localizedUiText(context: Context, language: String, text: String, vararg args: Any): String {
    val id = sharedStringIds[text] ?: return text
    val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
    return context.createConfigurationContext(config).getString(id, *args)
}

@Composable
fun tr(text: String, vararg args: Any): String {
    val context = LocalContext.current
    val language = LocalLanguage.current
    val configuration = LocalConfiguration.current
    val localized = remember(context, language, configuration) {
        context.createConfigurationContext(Configuration(configuration).apply { setLocale(Locale.forLanguageTag(language)) })
    }
    val id = sharedStringIds[text] ?: return text
    return localized.getString(id, *args)
}

val launchLanguages =
    listOf(
        "en" to "English (U.S.)",
        "hi" to "हिन्दी",
        "ta" to "தமிழ்",
        "id" to "Bahasa Indonesia",
        "ar" to "العربية",
        "fr" to "Français",
        "es" to "Español",
        "pt" to "Português (BR)",
        "tr" to "Türkçe",
        "de" to "Deutsch",
    )
