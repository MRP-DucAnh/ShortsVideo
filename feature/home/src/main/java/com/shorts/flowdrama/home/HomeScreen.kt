package com.shorts.flowdrama.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import com.shorts.flowdrama.ads.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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
) {
    var hour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    LaunchedEffect(Unit) {
        while (true) {
            hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            delay(60_000)
        }
    }
    val daypart = DaypartResolver().resolve(hour)
    val rails = remember(catalog) { BuildHomeRails()(catalog) }
    val hero = catalog.shows.find { it.id == catalog.heroShowId } ?: catalog.shows.firstOrNull()
    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
        item(key = "header", contentType = "header") {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Wordmark(Modifier.weight(1f))
                Glyph(Icons.Outlined.Search, "Search", onSearch)
                Glyph(Icons.Outlined.NotificationsNone, "Notifications", onNotifications)
            }
            Text(
                tr(daypart.title),
                Modifier.padding(start = 16.dp, top = 8.dp),
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date()) +
                    " · " +
                    daypart.subtitle,
                Modifier.padding(start = 16.dp, top = 4.dp, bottom = 12.dp),
                fontSize = 12.sp,
                color = Sunset.Text3,
            )
        }
        if (hero != null)
            item(key = "hero", contentType = "hero") {
                Box(
                    Modifier.padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(238.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(artwork(hero.id))
                        .clickable { onShow(hero.id) }
                ) {
                    AsyncImage(
                        hero.posterUrl,
                        showTitle(hero),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(Modifier.fillMaxSize().background(Sunset.Scrim))
                    Text(
                        daypart.badge,
                        Modifier.padding(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Sunset.Gradient)
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Sunset.OnAccent,
                    )
                    Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Text(
                            showTitle(hero),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Sunset.Text,
                        )
                        Text(
                            "${hero.episodeCount} eps · ${hero.status} · ${hero.tagIds.take(2).joinToString(" · ")}",
                            fontSize = 11.sp,
                            color = Sunset.Text2,
                        )
                    }
                }
            }
        item(key = "dayparts", contentType = "cards") {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                listOf(
                        "07:00 · MORNING SIP" to "2-min recaps of what you watched",
                        "12:30 · LUNCH SPLASH" to "One short arc · fits in 20 min",
                    )
                    .forEach { (title, subtitle) ->
                        Column(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, Sunset.Border, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                title,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Sunset.Text3,
                            )
                            Text(
                                subtitle,
                                Modifier.padding(top = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
            }
        }
        item(key = "tonight", contentType = "rail") {
            SectionTitle("Also flowing tonight", "See all", onBrowse)
            PosterRail(catalog.shows, onShow)
        }
        if (history.isNotEmpty())
            item(key = "continue", contentType = "rail") {
                SectionTitle("Continue watching")
                PosterRail(
                    history.mapNotNull { p -> catalog.shows.find { it.id == p.showId } },
                    onShow,
                )
            }
        item(key = "ad", contentType = "ad") { HomeNative(ads) }
        items(rails, key = { "rail:${it.tag.id}" }, contentType = { "rail" }) { rail ->
            SectionTitle(LocalizedTextResolver().resolve(rail.tag.name, LocalLanguage.current))
            PosterRail(rail.shows, onShow)
        }
    }
}

@Composable
private fun PosterRail(shows: List<Show>, onShow: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(shows, key = { it.id }, contentType = { "poster" }) { show ->
            Poster(show, Modifier.size(104.dp, 138.dp)) { onShow(show.id) }
        }
    }
}
