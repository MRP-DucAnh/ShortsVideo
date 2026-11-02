package com.shorts.flowdrama.domain

import org.junit.Assert.*
import org.junit.Test

class PoliciesTest {
    private val policy = AdsPolicy()

    @Test
    fun globalKillOverridesPlacement() {
        assertFalse(policy.allows(AdsConfig(enable = false), "1.0.0", "native_home"))
    }

    @Test
    fun versionKillOverridesPlacement() {
        assertFalse(
            policy.allows(AdsConfig(disabledVersions = setOf("1.0.0")), "1.0.0", "native_home")
        )
    }

    @Test
    fun versionMatchingIsExact() {
        assertTrue(
            policy.allows(AdsConfig(disabledVersions = setOf("1.0")), "1.0.0", "native_home")
        )
    }

    @Test
    fun disabledPlacementDoesNotDisableOthers() {
        val config = AdsConfig(placements = mapOf("native_home" to false, "banner_library" to true))
        assertFalse(policy.allows(config, "1.0.0", "native_home"))
        assertTrue(policy.allows(config, "1.0.0", "banner_library"))
    }

    @Test
    fun unknownPlacementIsDisabled() {
        assertFalse(policy.allows(AdsConfig(), "1.0.0", "unknown"))
    }

    @Test
    fun defaultConfigHasDeterministicPlacements() {
        assertTrue(policy.allows(AdsConfig(), "1.0.0", "interstitial_onboarding"))
    }

    @Test
    fun missingTranslationUsesDefaultLanguage() {
        assertEquals("English", LocalizedTextResolver().resolve(mapOf("en" to "English"), "ar"))
    }

    @Test
    fun blankTranslationIsNotDisplayed() {
        assertEquals(
            "English",
            LocalizedTextResolver().resolve(mapOf("ar" to " ", "en" to "English"), "ar"),
        )
    }

    @Test
    fun regionalLocaleUsesBaseTranslation() {
        assertEquals(
            "Olá",
            LocalizedTextResolver().resolve(mapOf("pt" to "Olá", "en" to "Hello"), "pt-BR"),
        )
    }

    private fun episode(sources: Map<String, Map<String, String>>) =
        Episode(7, true, 0, 4_000_000_000L, sources)

    @Test
    fun unknownQualityDoesNotDropEpisode() {
        val source =
            PlaybackSourceResolver()
                .resolve(
                    episode(mapOf("en" to mapOf("2160p" to "https://a/4k.mp4"))),
                    "ar",
                    "en",
                    "720p",
                )
        assertEquals("2160p", source?.quality)
    }

    @Test
    fun audioFallbackUsesDefaultLanguage() {
        val source =
            PlaybackSourceResolver()
                .resolve(
                    episode(
                        mapOf(
                            "en" to mapOf("720p" to "https://a/en.mp4"),
                            "hi" to mapOf("720p" to "https://a/hi.mp4"),
                        )
                    ),
                    "ar",
                    "hi",
                    "Auto",
                )
        assertEquals("hi", source?.language)
    }

    @Test
    fun unavailableSourceReturnsNull() {
        assertNull(PlaybackSourceResolver().resolve(episode(emptyMap()), "en", "en", "Auto"))
    }

    @Test
    fun longCountersArePreserved() {
        assertEquals(4_000_000_000L, episode(emptyMap()).likeCount)
    }

    @Test
    fun daypartUsesExplicitLocalHour() {
        val resolver = DaypartResolver()
        assertEquals(Daypart.Morning, resolver.resolve(7))
        assertEquals(Daypart.Lunch, resolver.resolve(12))
        assertEquals(Daypart.Night, resolver.resolve(21))
        assertEquals(Daypart.Night, resolver.resolve(2))
    }

    @Test
    fun unknownDurationDoesNotMarkCompleted() {
        assertFalse(Progress("a", 9, 90000, 0, 1).finished)
    }

    @Test
    fun completedEpisodeIsNotCompletedSeries() {
        assertFalse(Progress("a", 1, 99000, 100000, 1, false).finished)
    }

    @Test
    fun completedFinalEpisodeCompletesSeries() {
        assertTrue(Progress("a", 50, 99000, 100000, 1, true).finished)
    }

    @Test
    fun merchandisingSectionsDoNotRequireTagMembershipOnShows() {
        val shows =
            listOf(
                Show(
                    "1",
                    mapOf("en" to "A"),
                    emptyMap(),
                    emptyMap(),
                    "",
                    5,
                    "completed",
                    10,
                    listOf("romance"),
                ),
                Show(
                    "2",
                    mapOf("en" to "B"),
                    emptyMap(),
                    emptyMap(),
                    "",
                    8,
                    "unknown",
                    100,
                    listOf("romance"),
                ),
            )
        val tags =
            listOf(
                Tag("romance", emptyMap(), true, 1),
                Tag("popular", emptyMap(), false, 20),
                Tag("trending", emptyMap(), false, 10),
            )
        val rails = BuildHomeRails()(Catalog("en", listOf("en"), tags, "1", shows))
        assertEquals(listOf("trending", "popular"), rails.map { it.tag.id })
        assertEquals("2", rails.first().shows.first().id)
        assertEquals(2, rails.first().shows.size)
    }
}
