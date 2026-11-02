package com.shorts.flowdrama.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import com.shorts.flowdrama.designsystem.*
import com.shorts.flowdrama.domain.*

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Sunset.Background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Wordmark(size = 30)
            Spacer(Modifier.height(16.dp))
            Text("Short stories. Endless feelings.", color = Sunset.Text3, fontSize = 13.sp)
        }
    }
}

@Composable
fun LanguageScreen(
    language: String,
    followDevice: Boolean,
    onboarding: Boolean = false,
    onSelect: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit = {},
    poster: String? = null,
) {
    Box(Modifier.fillMaxSize()) {
        if (onboarding) {
            AsyncImage(
                poster,
                null,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = .35f,
            )
            Box(
                Modifier.fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                Sunset.Background.copy(alpha = .5f),
                                Sunset.Background,
                                Sunset.Background,
                            )
                        )
                    )
            )
        }
        Column(Modifier.fillMaxSize()) {
            if (onboarding) {
                Text(
                    "STEP 1 / 4",
                    Modifier.padding(start = 24.dp, top = 20.dp),
                    color = Sunset.Text3,
                    fontSize = 11.sp,
                )
                Text(
                    "Your language",
                    Modifier.padding(start = 24.dp, top = 12.dp),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            } else Heading("Language", onBack)
            Text(
                if (onboarding) "We matched this to your phone. Change it anytime in Settings."
                else "Applies to the entire app interface. Changes take effect immediately.",
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (!onboarding) {
                    LanguageRow(tr("Match device language"), followDevice) {
                        onSelect(language, true)
                    }
                    SectionTitle("Choose manually")
                }
                (if (onboarding) launchLanguages.sortedBy { if (it.first == language) 0 else 1 }
                    else launchLanguages)
                    .forEach { (code, name) ->
                        LanguageRow(name, language == code && (!followDevice || onboarding)) {
                            onSelect(code, false)
                        }
                    }
            }
            if (onboarding) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(4) {
                        Box(
                            Modifier.padding(4.dp)
                                .size(if (it == 0) 18.dp else 6.dp, 6.dp)
                                .clip(CircleShape)
                                .background(if (it == 0) Sunset.Accent else Sunset.Text4)
                        )
                    }
                }
                ActionButton("Continue", onContinue, Modifier.fillMaxWidth().padding(24.dp))
            }
        }
    }
}

@Composable
private fun LanguageRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, Modifier.weight(1f), fontSize = 16.sp)
        if (selected) Text("✓", color = Sunset.Accent, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
fun CinemaOnboarding(
    step: Int,
    poster: String?,
    selected: Set<String>,
    onTaste: (Set<String>) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onNotification: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            poster,
            null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = .55f,
        )
        Box(Modifier.fillMaxSize().background(Sunset.Scrim))
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "STEP ${step+2} / 4",
                    Modifier.weight(1f),
                    color = Sunset.Text3,
                    fontSize = 11.sp,
                )
                TextButton(onClick = onSkip) { Text(tr("Skip"), color = Sunset.Text2) }
            }
            Spacer(Modifier.weight(1f))
            when (step) {
                0 -> {
                    Text(
                        tr("Every episode open.\nThe story just flows."),
                        fontSize = 38.sp,
                        lineHeight = 43.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        tr(
                            "Thousands of short dramas, free from Ep. 1 to the finale. No coins, no plans, no dam in the middle of your night."
                        ),
                        color = Sunset.Text2,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    )
                    Row(
                        Modifier.padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("FULL SEASONS", "NO SIGN-UP", "2-MIN EPISODES").forEach {
                            Text(
                                it,
                                Modifier.border(1.dp, Sunset.Border, RoundedCornerShape(999.dp))
                                    .padding(9.dp),
                                fontSize = 9.sp,
                                color = Sunset.Text2,
                            )
                        }
                    }
                    ActionButton("Step into the flow", onNext, Modifier.fillMaxWidth())
                }
                1 -> {
                    Text(
                        tr("How should tonight feel?"),
                        fontSize = 32.sp,
                        lineHeight = 37.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        "One mood is all it takes. Fine-tune it later.",
                        Modifier.padding(vertical = 12.dp),
                        color = Sunset.Text2,
                    )
                    val moods =
                        listOf(
                            Triple("Rage", "🔥", setOf("revenge", "rebirth", "urban")),
                            Triple(
                                "Swoon",
                                "♥",
                                setOf("romance", "ceo", "marriage", "sweet-romance"),
                            ),
                            Triple("Cry", "☂", setOf("family", "fate")),
                            Triple("Thrill", "⚡", setOf("suspense", "horror", "action")),
                        )
                    moods.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { (name, emoji, tags) ->
                                val active = tags.any { it in selected }
                                Column(
                                    Modifier.weight(1f)
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (active) Sunset.Surface3
                                            else Sunset.Surface.copy(alpha = .9f)
                                        )
                                        .border(
                                            1.dp,
                                            if (active) Sunset.Accent else Sunset.Border,
                                            RoundedCornerShape(16.dp),
                                        )
                                        .clickable { onTaste(tags) }
                                        .padding(18.dp)
                                ) {
                                    Text(emoji, fontSize = 25.sp)
                                    Text(
                                        tr(name),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    ActionButton("Continue", onNext, Modifier.fillMaxWidth())
                }
                else -> {
                    Text(
                        tr("Your flow runs three times a day"),
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    listOf(
                            "☕  Morning sip · 07:00" to "2-min recaps of last night’s episodes",
                            "☀  Lunch splash · 12:30" to "One short arc for your lunch break",
                            "☾  Night flow · 21:00" to "A new story to get lost in",
                        )
                        .forEach { (title, subtitle) ->
                            Column(
                                Modifier.fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Sunset.Surface)
                                    .padding(16.dp)
                            ) {
                                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(subtitle, color = Sunset.Text3, fontSize = 12.sp)
                            }
                        }
                    Spacer(Modifier.height(24.dp))
                    ActionButton("Turn on notifications", onNotification, Modifier.fillMaxWidth())
                    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                        Text(tr("Not now"), color = Sunset.Text2)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(4) {
                    Box(
                        Modifier.padding(4.dp)
                            .size(if (it == step + 1) 18.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(if (it == step + 1) Sunset.Accent else Sunset.Text4)
                    )
                }
            }
        }
    }
}
