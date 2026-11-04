package com.shorts.flowdrama.designsystem

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.domain.Episode

@Composable
fun EpisodeGrid(
    episodes: List<Episode>,
    current: Int?,
    watched: Set<Int>,
    modifier: Modifier = Modifier,
    onEpisode: (Episode) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(episodes, key = { it.number }, contentType = { "episode" }) { episode ->
            val playing = current == episode.number
            Box(
                Modifier.fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (playing) Modifier.background(Sunset.Gradient)
                        else
                            Modifier.background(
                                if (episode.isFree) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                            )
                    )
                    .clickable { onEpisode(episode) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    episode.number.toString(),
                    color =
                        when {
                            playing -> Sunset.OnAccent
                            !episode.isFree -> Sunset.Text4
                            episode.number in watched -> Sunset.Text3
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    fontWeight = FontWeight.SemiBold,
                )
                if (!episode.isFree)
                    Icon(
                        FlowIcons.Lock,
                        tr("Episode locked"),
                        Modifier.align(Alignment.TopEnd).padding(4.dp).size(11.dp),
                        tint = Sunset.Gold,
                    )
            }
        }
    }
}
