package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DalalBlueLight,
    onPrimary = Color.Black,
    primaryContainer = DalalBlueDark,
    onPrimaryContainer = Color.White,
    secondary = BullGreen,
    onSecondary = Color.Black,
    secondaryContainer = BullGreenContainer,
    onSecondaryContainer = Color.White,
    tertiary = DalalGold,
    onTertiary = Color.Black,
    tertiaryContainer = DalalGoldContainer,
    onTertiaryContainer = Color.White,
    background = TerminalBgDark,
    onBackground = TerminalTextPrimaryDark,
    surface = TerminalSurfaceDark,
    onSurface = TerminalTextPrimaryDark,
    surfaceVariant = TerminalSurfaceVariantDark,
    onSurfaceVariant = TerminalTextSecondaryDark,
    outline = TerminalBorderDark,
    error = BearRed,
    onError = Color.White,
    errorContainer = BearRedContainer,
    onErrorContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DalalBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = BullGreenDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = DalalGold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    background = TerminalBgLight,
    onBackground = TerminalTextPrimaryLight,
    surface = TerminalSurfaceLight,
    onSurface = TerminalTextPrimaryLight,
    surfaceVariant = TerminalSurfaceVariantLight,
    onSurfaceVariant = TerminalTextSecondaryLight,
    outline = TerminalBorderLight,
    error = BearRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek dark financial terminal aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
