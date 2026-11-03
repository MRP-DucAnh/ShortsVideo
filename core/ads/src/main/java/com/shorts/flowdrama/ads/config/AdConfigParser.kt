package com.shorts.flowdrama.ads.config

import org.json.JSONArray
import org.json.JSONObject

/** Each Remote Config parameter is atomic. Invalid JSON retains that parameter's last good value. */
object AdConfigParser {
    val parameterKeys = setOf(
        "ads_config", "ad_unit_ids", "banner_native_ad_places", "banner_native_ad_places_2",
        "rewarded_and_inter_ad_places", "app_open_ad_places",
        "banner_ad_config", "native_ad_config", "interstitial_ad_config",
        "rewarded_ad_config", "rewarded_interstitial_ad_config", "app_open_ad_config",
    )

    fun parseIds(raw: String): Map<String, String> {
        val json = JSONObject(raw)
        return json.keys().asSequence().associateWith { key ->
            json.getString(key).also {
                require(it.isBlank() || it.matches(Regex("ca-app-pub-[0-9]+/[0-9]+"))) { "Invalid unit ID for $key" }
            }
        }
    }

    fun update(
        previous: AdConfiguration,
        values: Map<String, String>,
        onInvalid: (String) -> Unit = {},
    ): AdConfiguration {
        var result = previous
        parameterKeys.forEach { key ->
            if (key == "banner_native_ad_places_2") return@forEach
            if (key == "banner_native_ad_places") {
                result = updateInlinePlaces(result, values, onInvalid)
                return@forEach
            }
            val raw = values[key]?.takeIf { it.isNotBlank() } ?: return@forEach
            result = try {
                when (key) {
                    "ads_config" -> {
                        val json = JSONObject(raw)
                        val switches = json.optJSONObject("placements")
                        result.copy(
                            enabled = json.optBoolean("enable", true),
                            disabledVersions = json.optJSONArray("disabled_versions").strings().toSet(),
                            placementSwitches = switches?.keys()?.asSequence()?.associateWith {
                                switches.getJSONObject(it).optBoolean("enable", false)
                            }.orEmpty(),
                        )
                    }
                    "ad_unit_ids" -> result.copy(ids = previous.ids + parseIds(raw))
                    "rewarded_and_inter_ad_places" -> replacePlaces(result, raw,
                        setOf(AdFormat.Interstitial, AdFormat.Rewarded, AdFormat.RewardedInterstitial))
                    "app_open_ad_places" -> replacePlaces(result, raw, setOf(AdFormat.AppOpen))
                    else -> {
                        val format = AdFormat.fromKey(key.removeSuffix("_ad_config")) ?: error("Unknown policy")
                        result.copy(policies = result.policies + (format to parsePolicy(JSONObject(raw))))
                    }
                }
            } catch (_: Exception) {
                onInvalid(key)
                result
            }
        }
        return result
    }

    private fun updateInlinePlaces(config: AdConfiguration, values: Map<String, String>, onInvalid: (String) -> Unit): AdConfiguration {
        val formats = setOf(AdFormat.Banner, AdFormat.Native)
        val alternateKey = "banner_native_ad_places_2"
        val alternate = values[alternateKey]?.takeIf { it.isNotBlank() }
        if (alternate != null) {
            try {
                if (JSONArray(alternate).length() > 0) return replacePlaces(config, alternate, formats)
            } catch (_: Exception) { onInvalid(alternateKey) }
        }
        val primaryKey = "banner_native_ad_places"
        val primary = values[primaryKey]?.takeIf { it.isNotBlank() } ?: return config
        return try { replacePlaces(config, primary, formats) }
        catch (_: Exception) { onInvalid(primaryKey); config }
    }

    private fun replacePlaces(config: AdConfiguration, raw: String, formats: Set<AdFormat>): AdConfiguration {
        val array = JSONArray(raw)
        val places = (0 until array.length()).map { parsePlace(array.getJSONObject(it)) }
        require(places.all { it.format in formats })
        require(places.map { it.name }.distinct().size == places.size)
        val retained = config.placements.filterValues { it.format !in formats }
        require(places.none { it.name in retained })
        return config.copy(placements = retained + places.associateBy { it.name })
    }

    private fun parsePlace(json: JSONObject): AdPlacement {
        val format = requireNotNull(AdFormat.fromKey(json.getString("ad_type")))
        val name = json.getString("place_name").also { require(it.isNotBlank()) }
        val directId = if (json.has("ad_id")) json.getString("ad_id") else null
        if (directId != null) parseIds(JSONObject().put(name, directId).toString())
        return AdPlacement(
            name = name, format = format,
            idKey = json.optString("ad_id_key", format.key), adId = directId,
            enabled = json.optBoolean("is_enable", true),
            template = NativeTemplate.fromKey(json.optString("native_template_size", "small_cta_bottom")),
            bannerSize = BannerSize.fromKey(json.optString("banner_size", "anchored")),
            collapsible = json.optBoolean("is_collapsible", false),
            autoReload = json.optBoolean("is_auto_load_after_dismiss", false),
            ignoreInterval = json.optBoolean("is_ignore_interval", false),
            trackingClick = json.optBoolean("is_tracking_click", false),
            trackingShow = json.optBoolean("is_tracking_show", false),
            style = NativeAdStyle(
                background = json.color("background_color", "#241812"),
                headline = json.color("primary_text_color", "#FFFFFF"),
                body = json.color("body_text_color", "#C9BCAE"),
                ctaBackground = json.color("background_cta", "#FF7A3C"),
                ctaText = json.color("cta_text_color", "#1A0E05"),
                border = json.color("border_color", "#4A3325"),
                radiusDp = json.optInt("background_radius", 16).coerceIn(0, 48),
                ctaRadiusDp = json.optInt("cta_radius_dp", json.optInt("cta_radius", 24)).coerceIn(0, 48),
                fullBackground = json.color("background_full_color", "#160F0A"),
                mediaBackground = json.color("media_background_color", "#160F0A"),
                ctaBorder = json.color("cta_border_color", json.optString("background_cta", "#FF7A3C")),
            ),
        )
    }

    private fun parsePolicy(json: JSONObject) = AdFormatPolicy(
        retryEnabled = json.optBoolean("is_enable_retry", true),
        maxRetries = json.optInt("max_retry_count", 2).coerceIn(0, 5),
        retrySeconds = json.optJSONArray("retry_interval_second_list")?.let { array ->
            (0 until array.length()).map { array.getLong(it).coerceIn(0, 60) }
        } ?: listOf(1, 2, 3),
        intervalSeconds = json.optLong("time_interval", 0).coerceAtLeast(0),
        adsPerSession = json.optInt("ads_per_session", Int.MAX_VALUE).coerceAtLeast(0),
        sessionSeconds = json.optLong("time_per_session", 600).coerceAtLeast(1),
    )

    private fun JSONArray?.strings() = this?.let { (0 until length()).map { getString(it) } }.orEmpty()
    private fun JSONObject.color(key: String, fallback: String) = optString(key, fallback).also {
        require(it.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))) { "Invalid color: $key" }
    }
}
