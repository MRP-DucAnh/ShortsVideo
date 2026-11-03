package com.shorts.flowdrama.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shorts.flowdrama.designsystem.Poster
import com.shorts.flowdrama.domain.Show

@Composable
internal fun HomePosterRail(shows: List<Show>, onShow: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(shows, key = { it.id }, contentType = { "poster" }) { show ->
            Poster(show, Modifier.size(104.dp, 138.dp)) { onShow(show.id) }
        }
    }
}
