package org.f7.motoactions.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Amber / charcoal Mot-Actions palette — avoids stock purple Material defaults. */
private val Amber = Color(0xFFE8A23A)
private val AmberDim = Color(0xFFC4842A)
private val Ink = Color(0xFF12141A)
private val InkElevated = Color(0xFF1C1F28)
private val Mist = Color(0xFFF3F0EA)
private val MistCard = Color(0xFFFFFFFF)
private val Slate = Color(0xFF5C6370)
private val OnAmber = Color(0xFF1A1205)

private val DarkColors =
    darkColorScheme(
        primary = Amber,
        onPrimary = OnAmber,
        primaryContainer = Color(0xFF3D2E14),
        onPrimaryContainer = Color(0xFFFFE0A8),
        secondary = Color(0xFF9AA3B5),
        onSecondary = Ink,
        background = Ink,
        onBackground = Color(0xFFE8EAF0),
        surface = InkElevated,
        onSurface = Color(0xFFE8EAF0),
        surfaceVariant = Color(0xFF2A2F3A),
        onSurfaceVariant = Color(0xFFB8BECB),
        outline = Color(0xFF4A5160),
        error = Color(0xFFFF8A80),
    )

private val LightColors =
    lightColorScheme(
        primary = AmberDim,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFE8C2),
        onPrimaryContainer = Color(0xFF3D2A08),
        secondary = Slate,
        onSecondary = Color.White,
        background = Mist,
        onBackground = Color(0xFF1A1C22),
        surface = MistCard,
        onSurface = Color(0xFF1A1C22),
        surfaceVariant = Color(0xFFE7E2D8),
        onSurfaceVariant = Color(0xFF4A4F5A),
        outline = Color(0xFFC4BDB0),
        error = Color(0xFFB3261E),
    )

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromPref(v: Int): ThemeMode =
            when (v) {
                1 -> LIGHT
                2 -> DARK
                else -> SYSTEM
            }
    }

    fun toPref(): Int =
        when (this) {
            SYSTEM -> 0
            LIGHT -> 1
            DARK -> 2
        }
}

@Composable
fun F7MotoActionsTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark =
        when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    val typography =
        MaterialTheme.typography.copy(
            headlineLarge =
                TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp,
                ),
            headlineSmall =
                TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.2).sp,
                ),
            titleMedium =
                TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                ),
            bodyMedium =
                TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
            bodySmall =
                TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                ),
            labelLarge =
                TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                ),
        )
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = typography,
        content = content,
    )
}
