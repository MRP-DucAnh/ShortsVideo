package com.shorts.flowdrama.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*

@Composable
internal fun HomeHero(hero: Show, daypart: Daypart, tags: List<Tag>, onShow: (String) -> Unit) {
    val context = LocalContext.current
    Box(
        Modifier.padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(238.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(artwork(hero.id))
            .clickable { onShow(hero.id) }
    ) {
        LoadingImage(
            remember(context, hero.posterUrl) { homeHeroRequest(context, hero.posterUrl) },
            showTitle(hero),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(Sunset.Scrim))
        Text(
            homeString(daypart.badgeResource()),
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
                heroMetadata(hero, tags),
                fontSize = 11.sp,
                color = Sunset.Text2,
            )
        }
    }
}
