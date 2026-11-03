package com.shorts.flowdrama.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.Sunset
import com.shorts.flowdrama.feature.home.R

@Composable
internal fun HomeDaypartCards() {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        listOf(
                homeString(R.string.home_morning_card) to homeString(R.string.home_morning_card_detail),
                homeString(R.string.home_lunch_card) to homeString(R.string.home_lunch_card_detail),
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
