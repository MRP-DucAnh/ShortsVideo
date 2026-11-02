package com.shorts.flowdrama.onboarding.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.Sunset
import com.shorts.flowdrama.feature.onboarding.R
import com.shorts.flowdrama.onboarding.onboardingString

/** A header, one scrollable content area, and a pinned footer. No per-screen spacing hacks. */
@Composable
internal fun OnboardingPage(
    step: Int,
    tag: String,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: () -> Unit = {},
    actionEnabled: Boolean = true,
    bottomAlignedContent: Boolean = false,
    saveError: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().testTag(tag)) {
        OnboardingBackground()
        Column(
            Modifier.fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp)
        ) {
            Text(
                onboardingString(R.string.ob_step, step, 4),
                color = Sunset.Text3,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment =
                    if (bottomAlignedContent) Alignment.BottomStart else Alignment.TopStart,
            ) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    content = content,
                )
            }
            Spacer(Modifier.height(5.dp))
            if (saveError)
                Text(
                    onboardingString(R.string.ob_save_error),
                    color = Sunset.Error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            OnboardingFooter(
                step,
                action,
                onAction,
                secondaryAction,
                onSecondaryAction,
                actionEnabled,
            )
        }
    }
}

@Composable
internal fun OnboardingBackground() {
    // Composite colors of the mockup's warm artwork + vertical scrim. No unlicensed poster.
    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF432D14),
                    .34f to Color(0xFF21160D),
                    .58f to Sunset.Background,
                    1f to Sunset.Background,
                )
            )
    )
}

@Composable
internal fun OnboardingHeadline(title: String, description: String) {
    Text(
        title,
        color = Sunset.Text,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-.5).sp,
    )
    Spacer(Modifier.height(10.dp))
    Text(description, color = Sunset.Text2, fontSize = 14.sp, lineHeight = 22.sp)
}

@Composable
internal fun OnboardingFooter(
    step: Int,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: () -> Unit = {},
    actionEnabled: Boolean = true,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OnboardingProgress(step)
        Spacer(Modifier.height(18.dp))
        OnboardingPrimaryButton(action, onAction, actionEnabled)
        if (secondaryAction != null)
            TextButton(
                onClick = onSecondaryAction,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(secondaryAction, color = Sunset.Text3, fontSize = 13.sp, lineHeight = 18.sp)
            }
    }
}

@Composable
internal fun OnboardingProgress(step: Int) {
    val description = onboardingString(R.string.ob_step, step, 4)
    Row(
        Modifier.testTag("onboarding-progress").semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(4) { index ->
            Box(
                Modifier.size(if (index == step - 1) 20.dp else 6.dp, 6.dp)
                    .clip(CircleShape)
                    .background(if (index == step - 1) Sunset.Accent else Sunset.Surface3)
            )
        }
    }
}

@Composable
internal fun OnboardingPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 50.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) Sunset.Gradient
                    else Brush.horizontalGradient(listOf(Sunset.Surface3, Sunset.Surface3))
                ),
        shape = CircleShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Sunset.OnAccent,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Sunset.Text3,
            ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
