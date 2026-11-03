package com.shorts.flowdrama.ads.config

import android.content.Context
import androidx.annotation.XmlRes
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.xmlpull.v1.XmlPullParser

/** JSON IDs -> packaged XML parameters -> activated Firebase parameters, in that order. */
interface AdConfigurationSource {
    val state: StateFlow<AdConfiguration>
    val invalidParameters: StateFlow<Set<String>>
    fun refresh()
}

class RemoteAdConfiguration(context: Context, @XmlRes defaultsResource: Int, debug: Boolean) : AdConfigurationSource {
    private val remote = FirebaseRemoteConfig.getInstance()
    private val defaults = readDefaults(context, defaultsResource)
    private val defaultIds = context.assets.open("ads/ads_ids_default.json").bufferedReader().use {
        AdConfigParser.parseIds(it.readText())
    }
    private val mutable = MutableStateFlow(AdConfigParser.update(AdConfiguration(ids = defaultIds), defaults))
    override val state = mutable.asStateFlow()
    private val mutableInvalid = MutableStateFlow<Set<String>>(emptySet())
    override val invalidParameters = mutableInvalid.asStateFlow()

    init {
        remote.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (debug) 0 else 3600)
            .setFetchTimeoutInSeconds(8).build())
        remote.setDefaultsAsync(defaults).addOnCompleteListener {
            applyActivated()
            refresh()
        }
    }

    override fun refresh() {
        remote.fetchAndActivate().addOnCompleteListener { applyActivated() }
    }

    private fun applyActivated() {
        val invalid = mutableSetOf<String>()
        // Rebase valid ID maps on packaged IDs so removing an override restores its default.
        val ids = runCatching { defaultIds + AdConfigParser.parseIds(remote.getString("ad_unit_ids")) }
            .getOrElse { invalid += "ad_unit_ids"; mutable.value.ids }
        val values = AdConfigParser.parameterKeys.filterNot { it == "ad_unit_ids" }
            .associateWith { remote.getString(it) }
        mutable.value = AdConfigParser.update(mutable.value.copy(ids = ids), values) { invalid += it }
        mutableInvalid.value = invalid
    }

    private fun readDefaults(context: Context, @XmlRes resource: Int): Map<String, String> {
        val result = mutableMapOf<String, String>()
        context.resources.getXml(resource).use { parser ->
            var key = ""
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) when (parser.name) {
                    "key" -> key = parser.nextText()
                    "value" -> result[key] = parser.nextText().trim()
                }
                parser.next()
            }
        }
        return result
    }
}
