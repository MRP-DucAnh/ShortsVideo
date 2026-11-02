package com.shorts.flowdrama.onboarding

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shorts.flowdrama.designsystem.Sunset
import com.shorts.flowdrama.feature.onboarding.R

@Composable
fun SplashScreen() {
    Box(
        Modifier.fillMaxSize().testTag("onboarding:splash").drawWithCache {
            val center = Offset(size.width * .5f, size.height * .42f)
            val horizontalRadius = size.width * .7f
            val verticalRadius = size.height * .5f
            val glow =
                Brush.radialGradient(
                    0f to Sunset.Accent.copy(alpha = .14f),
                    .7f to Color.Transparent,
                    1f to Color.Transparent,
                    center = center,
                    radius = horizontalRadius,
                )
            onDrawBehind {
                drawRect(Sunset.Background)
                withTransform({ scale(1f, verticalRadius / horizontalRadius, pivot = center) }) {
                    drawCircle(glow, radius = horizontalRadius, center = center)
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SplashAppMark()
            Spacer(Modifier.height(20.dp))
            SplashWordmark()
            Spacer(Modifier.height(12.dp))
            SplashSeriesBadge()
        }
    }
}

@Composable
private fun SplashAppMark() {
    Canvas(Modifier.size(78.dp)) {
        val rear =
            Path().apply {
                moveTo(size.width * .46f, size.height * .27f)
                lineTo(size.width * .84f, size.height * .50f)
                lineTo(size.width * .46f, size.height * .73f)
                close()
            }
        val front =
            Path().apply {
                moveTo(size.width * .18f, size.height * .31f)
                lineTo(size.width * .52f, size.height * .50f)
                lineTo(size.width * .18f, size.height * .69f)
                close()
            }
        drawPath(rear, Sunset.Gradient)
        drawPath(front, Color.White)
    }
}

@Composable
private fun SplashWordmark() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row {
            Text(
                "FLOW",
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "DRAMA",
                style =
                    TextStyle(
                        brush = Sunset.Gradient,
                        fontSize = 30.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 6.sp,
                    ),
            )
        }
    }
}

@Composable
private fun SplashSeriesBadge() {
    Text(
        onboardingString(R.string.ob_series_badge),
        modifier =
            Modifier.background(Sunset.Surface.copy(alpha = .65f), CircleShape)
                .border(1.dp, Sunset.Border, CircleShape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Sunset.Text3,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.7.sp,
    )
}
