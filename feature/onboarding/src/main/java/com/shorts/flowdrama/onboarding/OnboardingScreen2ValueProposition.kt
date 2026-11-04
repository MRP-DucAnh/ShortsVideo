package com.shorts.flowdrama.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.Sunset
import com.shorts.flowdrama.feature.onboarding.R
import com.shorts.flowdrama.onboarding.components.*

@Composable
internal fun OnboardingScreen2ValueProposition(onContinue: () -> Unit) {
    OnboardingPage(
        step = 2,
        tag = "onboarding:value",
        action = onboardingString(R.string.ob_enter_flow),
        onAction = onContinue,
        bottomAlignedContent = true,
        background = { ValuePropositionArtwork() },
    ) {
        OnboardingHeadline(
            onboardingString(R.string.ob_value_title),
            onboardingString(R.string.ob_value_description),
        )
        Spacer(Modifier.height(18.dp))
        ValuePromises()
        Spacer(Modifier.height(22.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ValuePromises() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(R.string.ob_full_seasons, R.string.ob_no_signup, R.string.ob_short_episodes)
            .forEach { label ->
                Text(
                    onboardingString(label),
                    Modifier.background(Color.White.copy(alpha = .10f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = .16f), CircleShape)
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Sunset.Text,
                )
            }
    }
}

@Composable
private fun ValuePropositionArtwork() {
    Box(Modifier.fillMaxSize().background(Sunset.Background)) {
        // Artwork occupies 70% of the screen; OnboardingPage draws text above this layer.
        Box(Modifier.fillMaxWidth().fillMaxHeight(.70f)) {
            Image(
                painterResource(R.drawable.onboarding_golden_hour_full),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = androidx.compose.ui.Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to Sunset.Background.copy(alpha = .2f),
                .2f to Color.Transparent,
                .65f to Color.Transparent,
                .85f to Sunset.Background.copy(alpha = .4f),
                1f to Sunset.Background,
            )))
        }
    }
}
