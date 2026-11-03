package com.shorts.flowdrama.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.Daypart
import com.shorts.flowdrama.feature.home.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun HomeHeader(daypart: Daypart, now: Long, onSearch: () -> Unit, onNotifications: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(Modifier.weight(1f))
        Glyph(Icons.Outlined.Search, homeString(R.string.home_search), onSearch)
        Glyph(Icons.Outlined.NotificationsNone, homeString(R.string.home_notifications), onNotifications)
    }
    Text(
        homeString(daypart.titleResource()),
        Modifier.padding(start = 16.dp, top = 8.dp),
        fontSize = 25.sp,
        fontWeight = FontWeight.ExtraBold,
    )
    Text(
        SimpleDateFormat("HH:mm", Locale.forLanguageTag(LocalLanguage.current)).format(Date(now)) +
            " · " +
            homeString(daypart.subtitleResource()),
        Modifier.padding(start = 16.dp, top = 4.dp, bottom = 12.dp),
        fontSize = 12.sp,
        color = Sunset.Text3,
    )
}
