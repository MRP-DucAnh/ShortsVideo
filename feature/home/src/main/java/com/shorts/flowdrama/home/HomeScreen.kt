package com.shorts.flowdrama.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shorts.flowdrama.ads.*
import com.shorts.flowdrama.ads.config.AdPlacements
import com.shorts.flowdrama.ads.compose.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.feature.home.R
import java.util.Calendar
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    catalog: Catalog,
    history: List<Progress>,
    ads: AdsController,
    onShow: (String) -> Unit,
    onSearch: () -> Unit,
    onBrowse: () -> Unit,
    onNotifications: () -> Unit,
    online: Boolean = true,
    onRetry: () -> Unit = {},
) {
    val now = rememberHomeTime()
    val daypart = DaypartResolver().resolve(
        Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    )
    if (!online) {
        // Scaffold already applies system insets; offline keeps only the shared app bar.
        Column(Modifier.fillMaxSize()) {
            HomeTopBar(onSearch, onNotifications)
            Box(Modifier.weight(1f).fillMaxWidth()) { NetworkErrorState(onRetry) }
        }
        return
    }
    val feedAd = rememberInlineAdState(ads, AdPlacements.HomeFeed)
    val rails = remember(catalog) { BuildHomeRails()(catalog) }
    val hero = catalog.homeHero()
    val continueWatching = remember(catalog, history) {
        val shows = catalog.shows.associateBy { it.id }
        history.mapNotNull { shows[it.showId] }.distinctBy { it.id }
    }
    LazyColumn(modifier = Modifier.clipToBounds().testTag("home:feed"), contentPadding = PaddingValues(bottom = 20.dp)) {
        item(key = "header", contentType = "header") {
            HomeHeader(daypart, now, onSearch, onNotifications)
        }
        if (hero != null) item(key = "hero", contentType = "hero") {
            HomeHero(hero, daypart, catalog.tags, onShow)
        }
        item(key = "dayparts", contentType = "cards") { HomeDaypartCards() }
        item(key = "tonight", contentType = "rail") {
            SectionTitle(homeString(R.string.home_also_flowing), homeString(R.string.home_see_all), onBrowse)
            HomePosterRail(catalog.shows, onShow)
        }
        if (continueWatching.isNotEmpty()) item(key = "continue", contentType = "rail") {
            SectionTitle(homeString(R.string.home_continue_watching))
            HomePosterRail(continueWatching, onShow)
        }
        item(key = "ad", contentType = "ad") { InlineAdSlot(feedAd, homeString(R.string.home_ad_attribution)) }
        items(rails, key = { "rail:${it.tag.id}" }, contentType = { "rail" }) { rail ->
            SectionTitle(LocalizedTextResolver().resolve(rail.tag.name, LocalLanguage.current))
            HomePosterRail(rail.shows, onShow)
        }
    }
}

@Composable
private fun rememberHomeTime(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000)
        }
    }
    return now
}
