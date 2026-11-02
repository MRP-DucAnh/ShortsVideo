package com.shorts.flowdrama.onboarding.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shorts.flowdrama.designsystem.Sunset

/** Shared selection/press treatment; each card keeps its own plainly named content composable. */
@Composable
internal fun SelectableOption(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, tween(120), label = "option-press")
    val selection by
        animateFloatAsState(if (selected) 1f else 0f, tween(180), label = "option-selection")
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    if (gradient)
                        listOf(
                            lerp(Sunset.Surface, Color(0xFFFFB020), selection),
                            lerp(Sunset.Surface, Color(0xFFFF6A3D), selection),
                            lerp(Sunset.Surface, Color(0xFFF5405F), selection),
                        )
                    else
                        listOf(
                            lerp(Sunset.Surface, Sunset.Accent, selection),
                            lerp(Sunset.Surface, Sunset.Accent, selection),
                        )
                )
            )
            .border(1.dp, if (selected) Sunset.Accent else Sunset.Border, shape)
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                interactionSource = interaction,
                indication = ripple(),
                onValueChange = { onClick() },
            )
    ) {
        content()
    }
}
