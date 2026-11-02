package com.shorts.flowdrama.designsystem

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import com.shorts.flowdrama.domain.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Sunset.Gradient)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(tr(text), color = Sunset.OnAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Glyph(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, tr(description), tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun Heading(
    title: String,
    back: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (back != null) Glyph(Icons.AutoMirrored.Outlined.ArrowBack, "Back", back)
        Text(
            tr(title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        trailing()
    }
}

@Composable
fun Wordmark(modifier: Modifier = Modifier, size: Int = 21) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(Modifier.size((size + 7).dp)) {
            val rear =
                Path().apply {
                    moveTo(this@Canvas.size.width * .43f, this@Canvas.size.height * .2f)
                    lineTo(this@Canvas.size.width * .94f, this@Canvas.size.height * .5f)
                    lineTo(this@Canvas.size.width * .43f, this@Canvas.size.height * .8f)
                    close()
                }
            drawPath(rear, Sunset.Gradient)
            val front =
                Path().apply {
                    moveTo(this@Canvas.size.width * .08f, this@Canvas.size.height * .25f)
                    lineTo(this@Canvas.size.width * .56f, this@Canvas.size.height * .55f)
                    lineTo(this@Canvas.size.width * .08f, this@Canvas.size.height * .85f)
                    close()
                }
            drawPath(front, Color.White)
        }
        Spacer(Modifier.width(7.dp))
        Text(
            "FLOW",
            fontSize = size.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = 2.sp,
        )
        Text(
            "DRAMA",
            style =
                androidx.compose.ui.text.TextStyle(
                    brush = Sunset.Gradient,
                    fontSize = size.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                ),
        )
    }
}

@Composable
fun Poster(
    show: Show,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val border =
        if (selected) Modifier.border(2.dp, Sunset.Gradient, RoundedCornerShape(10.dp))
        else Modifier
    Box(
        modifier
            .then(border)
            .clip(RoundedCornerShape(10.dp))
            .background(artwork(show.id))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            show.posterUrl,
            showTitle(show),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun showTitle(show: Show): String =
    LocalizedTextResolver().resolve(show.title, LocalLanguage.current)

@Composable
fun SectionTitle(title: String, action: String? = null, onClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tr(title),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (action != null)
            TextButton(onClick = onClick) {
                Text(
                    tr(action),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
    }
}

@Composable
fun ShowRow(
    show: Show,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Poster(show, Modifier.size(98.dp, 64.dp), onClick = onClick)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                showTitle(show),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) trailing()
        else Icon(Icons.Outlined.ChevronRight, null, tint = Sunset.Text3)
    }
}

@Composable
fun BlankState(
    title: String,
    message: String,
    action: String,
    icon: ImageVector = Icons.Outlined.BookmarkBorder,
    onClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icon, null, Modifier.padding(24.dp).size(40.dp), tint = Sunset.Text3)
        }
        Spacer(Modifier.height(24.dp))
        Text(tr(title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            tr(message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        ActionButton(action, onClick)
    }
}

@Composable
fun LoadingState() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Wordmark()
        Box(
            Modifier.fillMaxWidth()
                .height(238.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Box(
            Modifier.width(160.dp)
                .height(18.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                Box(
                    Modifier.weight(1f)
                        .height(148.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
        LinearProgressIndicator(Modifier.fillMaxWidth(), color = Sunset.Accent)
    }
}

@Composable
fun TagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(tr(label), fontSize = 12.sp) },
        shape = RoundedCornerShape(999.dp),
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                selectedLabelColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

fun compactCount(value: Long, language: String): String {
    val divisor =
        when {
            value >= 1_000_000_000 -> 1_000_000_000.0
            value >= 1_000_000 -> 1_000_000.0
            value >= 1_000 -> 1_000.0
            else -> 1.0
        }
    val suffix =
        when (divisor) {
            1_000_000_000.0 -> "B"
            1_000_000.0 -> "M"
            1_000.0 -> "K"
            else -> ""
        }
    return NumberFormat.getNumberInstance(Locale.forLanguageTag(language))
        .apply { maximumFractionDigits = if (divisor == 1.0) 0 else 1 }
        .format(value / divisor) + suffix
}
