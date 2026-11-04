package com.shorts.flowdrama.designsystem

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.domain.Episode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodePicker(
    episodes: List<Episode>,
    current: Int?,
    watched: Set<Int>,
    onDismiss: () -> Unit,
    onEpisode: (Episode) -> Unit,
) {
    val pages = episodes.chunked(20)
    var page by rememberSaveable(episodes.size, current) {
        mutableIntStateOf((episodes.indexOfFirst { it.number == current }.coerceAtLeast(0) / 20))
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("Episodes"), fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(tr("%1\$d eps · %2\$d watched", episodes.size,
                    watched.count { number -> episodes.any { it.number == number } }),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.horizontalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { index, group ->
                    TagChip("${group.first().number}–${group.last().number}", page == index) { page = index }
                }
            }
            EpisodeGrid(pages.getOrNull(page).orEmpty(), current, watched,
                Modifier.fillMaxWidth().heightIn(max = 340.dp), onEpisode)
        }
    }
}
