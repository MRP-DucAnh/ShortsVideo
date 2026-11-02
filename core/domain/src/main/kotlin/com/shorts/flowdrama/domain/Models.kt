package com.shorts.flowdrama.domain

import kotlinx.coroutines.flow.StateFlow

data class Tag(
    val id: String,
    val name: Map<String, String>,
    val isCategory: Boolean,
    val priority: Int,
)

data class Show(
    val id: String,
    val title: Map<String, String>,
    val synopsis: Map<String, String>,
    val hook: Map<String, String>,
    val posterUrl: String,
    val episodeCount: Int,
    val status: String,
    val viewCount: Long,
    val tagIds: List<String>,
)

data class Catalog(
    val defaultLanguage: String,
    val languages: List<String>,
    val tags: List<Tag>,
    val heroShowId: String,
    val shows: List<Show>,
) {
    val categories
        get() = tags.filter { it.isCategory }.sortedBy { it.priority }

    val rails
        get() = tags.filterNot { it.isCategory }.sortedBy { it.priority }
}

data class Episode(
    val number: Int,
    val isFree: Boolean,
    val durationSeconds: Long,
    val likeCount: Long,
    val sources: Map<String, Map<String, String>>,
)

data class Progress(
    val showId: String,
    val episodeNumber: Int,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val isLastEpisode: Boolean = false,
) {
    val finished
        get() = isLastEpisode && durationMs > 0 && positionMs >= durationMs * .95
}

data class Preferences(
    val language: String = "en",
    val followDevice: Boolean = true,
    val onboardingDone: Boolean = false,
    val tasteIds: Set<String> = emptySet(),
    val lightTheme: Boolean = false,
    val wifiPreviewOnly: Boolean = true,
    val speed: Float = 1f,
    val quality: String = "Auto",
    val audioLanguage: String? = null,
    val moodIds: Set<String> = emptySet(),
)

interface CatalogRepository {
    suspend fun catalog(refresh: Boolean = false): Catalog

    suspend fun episodes(showId: String): List<Episode>
}

interface LibraryRepository {
    val savedIds: StateFlow<Set<String>>
    val history: StateFlow<List<Progress>>

    suspend fun toggleSaved(showId: String)

    suspend fun saveProgress(progress: Progress)

    suspend fun clearHistory()

    suspend fun removeHistory(showId: String)
}

interface PreferencesRepository {
    val preferences: StateFlow<Preferences>

    suspend fun update(transform: (Preferences) -> Preferences)
}

class LocalizedTextResolver {
    // UI locales and available content locales differ; never display a blank translation.
    fun resolve(
        values: Map<String, String>,
        language: String,
        defaultLanguage: String = "en",
    ): String =
        values[language]?.takeIf { it.isNotBlank() }
            ?: values[language.substringBefore('-')]?.takeIf { it.isNotBlank() }
            ?: values[defaultLanguage]?.takeIf { it.isNotBlank() }
            ?: values["en"]?.takeIf { it.isNotBlank() }
            ?: values.values.firstOrNull { it.isNotBlank() }.orEmpty()
}

data class PlaybackSource(val language: String, val quality: String, val url: String)

class PlaybackSourceResolver {
    fun resolve(
        episode: Episode,
        language: String,
        defaultLanguage: String,
        quality: String,
    ): PlaybackSource? {
        val languages =
            listOf(language, language.substringBefore('-'), defaultLanguage, "en") +
                episode.sources.keys
        for (lang in languages.distinct()) {
            val available =
                episode.sources[lang]
                    ?.filterValues { it.startsWith("https://") }
                    ?.takeIf { it.isNotEmpty() } ?: continue
            val key =
                quality.takeIf { available.containsKey(it) }
                    ?: available.keys.maxByOrNull { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
                    ?: continue
            return PlaybackSource(lang, key, available.getValue(key))
        }
        return null
    }
}

enum class Daypart(val title: String, val subtitle: String, val badge: String) {
    Morning("Morning sip", "a little drama to start your day", "MORNING PICK"),
    Lunch("Lunch splash", "one short arc, a perfect little break", "LUNCH PICK"),
    Night("Night flow", "long episodes, lights off", "TONIGHT’S PICK"),
}

class DaypartResolver {
    fun resolve(hour: Int) =
        when (hour) {
            in 5..10 -> Daypart.Morning
            in 11..16 -> Daypart.Lunch
            else -> Daypart.Night
        }
}

data class AdsConfig(
    val enable: Boolean = true,
    val disabledVersions: Set<String> = emptySet(),
    val placements: Map<String, Boolean> =
        mapOf("interstitial_onboarding" to true, "native_home" to true, "banner_library" to true),
)

class AdsPolicy {
    fun allows(config: AdsConfig, versionName: String, placement: String) =
        config.enable &&
            versionName !in config.disabledVersions &&
            config.placements[placement] == true
}

class SearchShows(private val resolver: LocalizedTextResolver = LocalizedTextResolver()) {
    operator fun invoke(catalog: Catalog, query: String, language: String): List<Show> {
        val term = query.trim()
        if (term.isEmpty()) return emptyList()
        return catalog.shows.filter {
            resolver.resolve(it.title, language, catalog.defaultLanguage).contains(term, true) ||
                it.tagIds.any { tag -> tag.contains(term, true) }
        }
    }
}

data class HomeRail(val tag: Tag, val shows: List<Show>)

class BuildHomeRails {
    operator fun invoke(catalog: Catalog): List<HomeRail> =
        catalog.rails.map { tag ->
            // Merchandising tags define sections; fixtures do not put their IDs in show.tagIds.
            // Views are the only supplied popularity signal; no release dates exist for Newest.
            val shows =
                when (tag.id) {
                    "trending",
                    "popular" -> catalog.shows.sortedByDescending { it.viewCount }
                    else -> catalog.shows
                }
            HomeRail(tag, shows)
        }
}
