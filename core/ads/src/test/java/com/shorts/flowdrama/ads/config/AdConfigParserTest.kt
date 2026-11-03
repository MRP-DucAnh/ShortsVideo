package com.shorts.flowdrama.ads.config

import org.junit.Assert.*
import org.junit.Test

class AdConfigParserTest {
    private val base = AdConfiguration(ids = mapOf("native" to "ca-app-pub-1/1"),
        placements = mapOf("feed" to AdPlacement("feed", AdFormat.Native)))

    @Test fun globalKillWinsAndVersionMatchingIsExact() {
        assertFalse(base.copy(enabled = false).allows("feed", "1.0.0"))
        assertFalse(base.copy(disabledVersions = setOf("1.0.0")).allows("feed", "1.0.0"))
        assertTrue(base.copy(disabledVersions = setOf("1.0")).allows("feed", "1.0.0"))
    }

    @Test fun disablingOnePlacementDoesNotDisableOthers() {
        val config = base.copy(placements = base.placements + ("other" to AdPlacement("other", AdFormat.Native)),
            placementSwitches = mapOf("feed" to false))
        assertFalse(config.allows("feed", "1"))
        assertTrue(config.allows("other", "1"))
    }

    @Test fun remoteIdOverridesFallbackAndBlankIdDisablesPlacement() {
        val changed = AdConfigParser.update(base, mapOf("ad_unit_ids" to """{"native":"ca-app-pub-2/2"}"""))
        assertEquals("ca-app-pub-2/2", changed.unitId(changed.placements.getValue("feed")))
        assertTrue(changed.allows("feed", "1"))
        val disabled = AdConfigParser.update(changed, mapOf("ad_unit_ids" to """{"native":""}"""))
        assertFalse(disabled.allows("feed", "1"))
    }

    @Test fun malformedParameterRetainsLastGoodConfigWhileOtherParametersApply() {
        val errors = mutableListOf<String>()
        val changed = AdConfigParser.update(base, mapOf(
            "banner_native_ad_places" to "broken", "ad_unit_ids" to """{"native":"ca-app-pub-2/2"}"""
        ), errors::add)
        assertEquals(base.placements, changed.placements)
        assertEquals("ca-app-pub-2/2", changed.ids["native"])
        assertEquals(listOf("banner_native_ad_places"), errors)
    }

    @Test fun disabledVersionAndLegacyPlacementSwitchStillWin() {
        val values = mapOf("ads_config" to """{"enable":true,"disabled_versions":["2"],"placements":{"feed":{"enable":false}}}""")
        val config = AdConfigParser.update(base, values)
        assertFalse(config.allows("feed", "1"))
        assertFalse(config.allows("feed", "2"))
        assertFalse(config.allows("unknown", "1"))
    }

    @Test fun allSixFormatsAndReferenceTemplateKeysParse() {
        val placements = mapOf(
            "banner_native_ad_places" to """[{"place_name":"b","ad_type":"banner","banner_size":"inline"},{"place_name":"n","ad_type":"native","native_template_size":"medium_media_left"}]""",
            "rewarded_and_inter_ad_places" to """[{"place_name":"i","ad_type":"interstitial"},{"place_name":"r","ad_type":"rewarded"},{"place_name":"ri","ad_type":"rewarded_interstitial"}]""",
            "app_open_ad_places" to """[{"place_name":"o","ad_type":"app_open"}]""",
        )
        val config = AdConfigParser.update(AdConfiguration(), placements)
        assertEquals(AdFormat.entries.toSet(), config.placements.values.map { it.format }.toSet())
        assertEquals(NativeTemplate.MediumMediaLeft, config.placements.getValue("n").template)
        assertEquals(BannerSize.Inline, config.placements.getValue("b").bannerSize)
        NativeTemplate.entries.forEach { assertEquals(it, NativeTemplate.fromKey(it.key)) }
    }

    @Test fun placementDirectIdTakesPrecedenceAndUnknownTypeDoesNotReplaceGoodGroup() {
        val direct = AdPlacement("feed", AdFormat.Native, adId = "ca-app-pub-3/3")
        assertEquals("ca-app-pub-3/3", base.unitId(direct))
        val changed = AdConfigParser.update(base, mapOf("banner_native_ad_places" to
            """[{"place_name":"feed","ad_type":"unsupported"}]"""))
        assertEquals(base.placements, changed.placements)
    }

    @Test fun replacingOneGroupDoesNotDeleteOtherFormatsAndEmptyListDisablesGroup() {
        val mixed = base.copy(placements = base.placements + ("open" to AdPlacement("open", AdFormat.AppOpen)))
        val cleared = AdConfigParser.update(mixed, mapOf("banner_native_ad_places" to "[]"))
        assertEquals(setOf("open"), cleared.placements.keys)
    }

    @Test fun retryConfigurationIsBoundedAndColorsAreValidated() {
        val changed = AdConfigParser.update(base, mapOf("native_ad_config" to
            """{"max_retry_count":999,"retry_interval_second_list":[-1,999]}"""))
        assertEquals(5, changed.policy(AdFormat.Native).maxRetries)
        assertEquals(listOf(0L,60L), changed.policy(AdFormat.Native).retrySeconds)
        val invalid = AdConfigParser.update(base, mapOf("banner_native_ad_places" to
            """[{"place_name":"feed","ad_type":"native","background_color":"bad-color"}]"""))
        assertEquals(base.placements, invalid.placements)
    }
    @Test fun alternateInlineListOverridesPrimaryAndEmptyListFallsBack() {
        val primary = """[{"place_name":"home_feed_after_continue_watching","ad_type":"native","background_cta":"#FF7A3C"}]"""
        val alternate = """[{"place_name":"home_feed_after_continue_watching","ad_type":"banner","banner_size":"standard_large"}]"""
        val overridden = AdConfigParser.update(base, mapOf("banner_native_ad_places" to primary,
            "banner_native_ad_places_2" to alternate))
        assertEquals(AdFormat.Banner, overridden.placements.getValue("home_feed_after_continue_watching").format)
        val fallback = AdConfigParser.update(overridden, mapOf("banner_native_ad_places" to primary,
            "banner_native_ad_places_2" to "[]"))
        assertEquals(AdFormat.Native, fallback.placements.getValue("home_feed_after_continue_watching").format)
    }

    @Test fun malformedAlternateFallsBackToPrimaryAndReportsTheCorrectParameter() {
        val errors = mutableListOf<String>()
        val changed = AdConfigParser.update(base, mapOf("banner_native_ad_places_2" to "broken",
            "banner_native_ad_places" to """[{"place_name":"home_feed_after_continue_watching","ad_type":"native"}]"""), errors::add)
        assertEquals(setOf("home_feed_after_continue_watching"), changed.placements.keys)
        assertEquals(listOf("banner_native_ad_places_2"), errors)
    }

    @Test fun referenceStyleAndTrackingFieldsAreParsedWithoutRenaming() {
        val config = AdConfigParser.update(base, mapOf("banner_native_ad_places" to """[{
            "place_name":"home_feed_after_continue_watching","ad_type":"native",
            "ad_id":"ca-app-pub-1/1","is_tracking_click":true,"is_tracking_show":true,
            "background_full_color":"#000000","background_color":"#112233",
            "primary_text_color":"#FFFFFF","body_text_color":"#CCCCCC",
            "background_cta":"#8400FF","cta_text_color":"#FFFFFF",
            "cta_border_color":"#AAAAAA","media_background_color":"#222222",
            "border_color":"#D1D1D1","cta_radius_dp":7,"background_radius":12
        }]"""))
        val place = config.placements.getValue("home_feed_after_continue_watching")
        assertTrue(place.trackingClick)
        assertTrue(place.trackingShow)
        assertEquals("#8400FF", place.style.ctaBackground)
        assertEquals("#000000", place.style.fullBackground)
        assertEquals("#222222", place.style.mediaBackground)
        assertEquals("#AAAAAA", place.style.ctaBorder)
        assertEquals(7, place.style.ctaRadiusDp)
    }

}
