package com.shorts.flowdrama.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.Sunset
import com.shorts.flowdrama.domain.*
import com.shorts.flowdrama.feature.onboarding.R
import com.shorts.flowdrama.onboarding.components.*

@Composable
internal fun OnboardingScreen3TastePicker(
    moodIds: Set<String>,
    tagIds: Set<String>,
    categories: List<Tag>,
    saveError: Boolean,
    onMood: (TasteMood) -> Unit,
    onGenre: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    OnboardingPage(
        step = 3,
        tag = "onboarding:taste",
        action = onboardingString(R.string.ob_continue),
        onAction = onContinue,
        secondaryAction = onboardingString(R.string.ob_skip),
        onSecondaryAction = onSkip,
        saveError = saveError,
    ) {
        OnboardingHeadline(
            onboardingString(R.string.ob_taste_title),
            onboardingString(R.string.ob_taste_description),
        )
        Spacer(Modifier.height(20.dp))
        MoodOptions(moodIds, onMood)
        Spacer(Modifier.height(18.dp))
        FineTuneGenres(categories, tagIds, onGenre)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MoodOptions(selected: Set<String>, onMood: (TasteMood) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TasteMood.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { mood ->
                    MoodOption(mood, mood.id in selected, { onMood(mood) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoodOption(
    mood: TasteMood,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val foreground = if (selected) Sunset.OnAccent else Sunset.Text
    SelectableOption(selected, onClick, modifier.testTag("mood:${mood.id}"), gradient = true) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(mood.emoji, fontSize = 24.sp, lineHeight = 28.sp)
            Text(
                onboardingString(mood.titleResource()),
                Modifier.padding(top = 5.dp),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = foreground,
            )
            Text(
                onboardingString(mood.detailResource()),
                Modifier.padding(top = 1.dp),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.Black else Sunset.Text3,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FineTuneGenres(
    categories: List<Tag>,
    selected: Set<String>,
    onGenre: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Text(
        onboardingString(R.string.ob_fine_tune),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Sunset.Text3,
        letterSpacing = .5.sp,
    )
    Spacer(Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val visible = if (expanded) categories else categories.take(6)
        visible.forEach { tag -> GenreOption(tag, tag.id in selected) { onGenre(tag.id) } }
        if (categories.size > 6)
            Box(
                Modifier.heightIn(min = 48.dp)
                    .testTag("genres:expand")
                    .clickable(role = Role.Button) { expanded = !expanded },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (expanded) onboardingString(R.string.ob_fewer_genres)
                    else onboardingString(R.string.ob_more_genres, categories.size - 6),
                    Modifier.clip(CircleShape)
                        .background(Sunset.Surface.copy(alpha = .6f))
                        .border(1.dp, Sunset.Border, CircleShape)
                        .padding(horizontal = 13.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Medium,
                    color = Sunset.Text2,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
    }
}

@Composable
private fun GenreOption(tag: Tag, selected: Boolean, onClick: () -> Unit) {
    // 48dp hit area surrounds a compact pill, preserving the mockup and touch accessibility.
    Box(
        Modifier.heightIn(min = 48.dp)
            .testTag("genre:${tag.id}")
            .toggleable(selected, role = Role.Checkbox, onValueChange = { onClick() }),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            genreTitle(tag),
            Modifier.clip(CircleShape)
                .background(if (selected) Sunset.Accent else Sunset.Surface.copy(alpha = .6f))
                .border(1.dp, if (selected) Sunset.Accent else Sunset.Border, CircleShape)
                .padding(horizontal = 13.dp, vertical = 8.dp),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Sunset.OnAccent else Sunset.Text2,
        )
    }
}
