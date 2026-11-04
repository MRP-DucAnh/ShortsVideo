package com.shorts.flowdrama.designsystem

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

object Sunset {
    val Background = Color(0xFF160F0A)
    val Surface = Color(0xFF241812)
    val Surface2 = Color(0xFF2F2018)
    val Surface3 = Color(0xFF3B2A1E)
    val Text = Color.White
    val Text2 = Color(0xFFC9BCAE)
    val Text3 = Color(0xFF8F8172)
    val Text4 = Color(0xFF5E5347)
    val Accent = Color(0xFFFF7A3C)
    val Pressed = Color(0xFFE5602A)
    val OnAccent = Color(0xFF1A0E05)
    val Gold = Color(0xFFE0B44C)
    val Success = Color(0xFF2ECC71)
    val Error = Color(0xFFFF453A)
    val Border = Color(0xFF4A3325)
    val Gradient =
        Brush.horizontalGradient(listOf(Color(0xFFFFB020), Color(0xFFFF6A3D), Color(0xFFF5405F)))
    val Scrim =
        Brush.verticalGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = .45f), Background.copy(alpha = .98f))
        )
}

val LocalLanguage = staticCompositionLocalOf { "en" }

@Composable
fun FlowTheme(language: String, light: Boolean = false, content: @Composable () -> Unit) {
    val dark =
        darkColorScheme(
            primary = Sunset.Accent,
            onPrimary = Sunset.OnAccent,
            background = Sunset.Background,
            surface = Sunset.Surface,
            surfaceVariant = Sunset.Surface2,
            onBackground = Sunset.Text,
            onSurface = Sunset.Text,
            onSurfaceVariant = Sunset.Text2,
            outline = Sunset.Border,
            error = Sunset.Error,
        )
    val scheme =
        if (light)
            lightColorScheme(
                primary = Color(0xFFAD431B),
                onPrimary = Color.White,
                background = Color(0xFFFFF8F2),
                surface = Color(0xFFFFF0E4),
                surfaceVariant = Color(0xFFF2E2D4),
                onBackground = Color(0xFF26170E),
                onSurface = Color(0xFF26170E),
                onSurfaceVariant = Color(0xFF665346),
                outline = Color(0xFFBCA28D),
            )
        else dark
    CompositionLocalProvider(
        LocalLanguage provides language,
        LocalLayoutDirection provides
            if (android.text.TextUtils.getLayoutDirectionFromLocale(java.util.Locale.forLanguageTag(language)) == android.view.View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

fun artwork(id: String): Brush {
    val palettes =
        listOf(
            0xFF5A2E1BL to 0xFF241209L,
            0xFF6B3A4AL to 0xFF2A1218L,
            0xFF3D4A2AL to 0xFF141A0CL,
            0xFF7A5220L to 0xFF2A1C0CL,
            0xFF2E4A5AL to 0xFF101C24L,
            0xFF5A2440L to 0xFF22101AL,
        )
    val pair = palettes[Math.floorMod(id.hashCode(), palettes.size)]
    return Brush.linearGradient(listOf(Color(pair.first), Color(pair.second)))
}
