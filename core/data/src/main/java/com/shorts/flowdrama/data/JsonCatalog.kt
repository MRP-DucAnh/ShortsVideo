package com.shorts.flowdrama.data

import com.shorts.flowdrama.domain.*
import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.textMap(): Map<String, String> =
    keys().asSequence().associateWith { optString(it) }

internal fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

class JsonCatalog {
    fun catalog(raw: String): Catalog {
        val root = JSONObject(raw)
        val tags =
            root.getJSONArray("tags").let { array ->
                (0 until array.length()).map { index ->
                    array.getJSONObject(index).let {
                        Tag(
                            it.getString("id"),
                            it.getJSONObject("name").textMap(),
                            it.optBoolean("isCategory"),
                            it.optInt("orderPriority"),
                        )
                    }
                }
            }
        val shows =
            root.getJSONArray("shows").let { array ->
                (0 until array.length()).map { index ->
                    array.getJSONObject(index).let {
                        Show(
                            it.getString("id"),
                            it.getJSONObject("title").textMap(),
                            it.getJSONObject("synopsis").textMap(),
                            it.optJSONObject("hook")?.textMap().orEmpty(),
                            it.getString("posterUrl"),
                            it.optInt("episodeCount"),
                            it.optString("status"),
                            it.optLong("viewCount"),
                            it.getJSONArray("tagIds").strings(),
                        )
                    }
                }
            }
        return Catalog(
            root.optString("defaultLanguage", "en"),
            root.getJSONArray("availableLanguages").strings(),
            tags,
            root.getJSONObject("home").getString("heroShowId"),
            shows,
        )
    }

    fun episodes(raw: String): List<Episode> {
        val array = JSONObject(raw).getJSONArray("episodes")
        return (0 until array.length()).map { index ->
            array.getJSONObject(index).let { item ->
                val sources = item.getJSONObject("sources")
                Episode(
                    item.getInt("number"),
                    item.optBoolean("isFree"),
                    item.optLong("durationSeconds"),
                    item.optLong("likeCount"),
                    sources.keys().asSequence().associateWith {
                        sources.getJSONObject(it).textMap()
                    },
                )
            }
        }
    }
}
