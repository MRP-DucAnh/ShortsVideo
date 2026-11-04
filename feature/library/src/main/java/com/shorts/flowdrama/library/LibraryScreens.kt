package com.shorts.flowdrama.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.ads.*
import com.shorts.flowdrama.ads.config.AdPlacements
import com.shorts.flowdrama.ads.compose.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.feature.library.R

@Composable
fun LibraryScreen(catalog: Catalog, history: List<Progress>, saved: Set<String>, ads: AdsController,
    onHistory: () -> Unit, onSaved: () -> Unit, onShow: (String) -> Unit) {
    val bottomAd = rememberInlineAdState(ads, AdPlacements.LibraryBottom)
    val recent = remember(catalog, history) {
        val shows = catalog.shows.associateBy { it.id }
        history.sortedByDescending { it.updatedAt }.mapNotNull { p -> shows[p.showId]?.let { p to it } }.take(10)
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(libraryString(R.string.library_title), Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp)).background(Sunset.Surface),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                Statistic(history.size, libraryString(R.string.library_watched), Modifier.weight(1f))
                Box(Modifier.height(36.dp).width(1.dp).background(Sunset.Border).align(Alignment.CenterVertically))
                Statistic(saved.size, libraryString(R.string.library_saved), Modifier.weight(1f))
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .clip(RoundedCornerShape(12.dp)).background(Sunset.Surface)) {
                NavigationRow(FlowIcons.History, libraryString(R.string.library_history), history.size, onHistory)
                HorizontalDivider(color = Sunset.Border, thickness = 1.dp)
                NavigationRow(FlowIcons.Bookmark, libraryString(R.string.library_saved), saved.size, onSaved)
            }
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(libraryString(R.string.library_recent), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(libraryString(R.string.library_see_all) + " ›", Modifier.clickable(onClick = onHistory), color = Sunset.Text3, fontSize = 13.sp)
            }
        }
        items(recent, key = { it.first.showId }) { (progress, show) ->
            LibraryShowRow(show, progressSubtitle(progress, show), { onShow(show.id) }, progress)
        }
        if (recent.isEmpty()) item { Text(libraryString(R.string.library_history_empty_message),
            Modifier.padding(16.dp), color = Sunset.Text3) }
        item { InlineAdSlot(bottomAd, adAttribution(LocalLanguage.current)) }
    }
}

@Composable
private fun Statistic(value: Int, label: String, modifier: Modifier) {
    Column(modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = MaterialTheme.colorScheme.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Sunset.Text3, fontSize = 11.sp)
    }
}

@Composable
private fun NavigationRow(icon: ImageVector, label: String, count: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = Sunset.Text2)
        Text(label, Modifier.weight(1f), fontSize = 14.sp, maxLines = 1)
        Text(count.toString(), color = Sunset.Text3, fontSize = 13.sp)
        Icon(FlowIcons.ChevronRight, null, Modifier.size(20.dp), tint = Sunset.Text4)
    }
}
