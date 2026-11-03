package com.shorts.flowdrama.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JsonCatalogEpisodeTitleTest {
    @Test fun parsesTitleAndTreatsMissingOrNullAsEmpty() {
        val raw = """
            {"episodes": [
              {"number":1,"title":"A new start","isFree":true,"sources":{}},
              {"number":2,"title":"","isFree":true,"sources":{}},
              {"number":3,"title":null,"isFree":true,"sources":{}},
              {"number":4,"isFree":true,"sources":{}}
            ]}
        """.trimIndent()
        assertEquals(listOf("A new start", "", "", ""), JsonCatalog().episodes(raw).map { it.title })
    }
}
