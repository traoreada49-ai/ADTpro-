package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

fun buildDarkColorScheme(palColor: AdtPaletteColor): ColorScheme {
    val primaryColor = if (palColor == AdtPaletteColor.VERT) AdtPrimary else palColor.primary
    val darkContainer = if (palColor == AdtPaletteColor.VERT) AdtPrimaryDark else palColor.darkVariant
    val accentColor = if (palColor == AdtPaletteColor.VERT) AdtAccent else palColor.lightVariant

    return darkColorScheme(
        primary = primaryColor,
        onPrimary = if (palColor == AdtPaletteColor.BLANCHE) Color.Black else Color.White,
        primaryContainer = darkContainer,
        onPrimaryContainer = Color.White,
        secondary = palColor.darkVariant,
        onSecondary = Color.White,
        tertiary = accentColor,
        background = if (palColor == AdtPaletteColor.NOIR) Color(0xFF080808) else DarkBg,
        onBackground = DarkTextPrimary,
        surface = if (palColor == AdtPaletteColor.NOIR) Color(0xFF101010) else DarkSurface,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkTextSecondary,
        outline = DarkBorder
    )
}

fun buildLightColorScheme(palColor: AdtPaletteColor): ColorScheme {
    val primaryColor = if (palColor == AdtPaletteColor.VERT) AdtPrimaryDark else if (palColor == AdtPaletteColor.BLANCHE) Color(0xFF2E2E2E) else palColor.primary
    val containerColor = if (palColor == AdtPaletteColor.VERT) AdtPrimary else if (palColor == AdtPaletteColor.BLANCHE) Color(0xFFEEEEEE) else palColor.lightVariant
    val accentColor = if (palColor == AdtPaletteColor.VERT) AdtAccent else palColor.lightVariant

    return lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = containerColor,
        onPrimaryContainer = Color.White,
        secondary = palColor.darkVariant,
        onSecondary = Color.White,
        tertiary = accentColor,
        background = LightBg,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightTextSecondary,
        outline = LightBorder
    )
}

@Composable
fun ADTProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    paletteColor: AdtPaletteColor = AdtPaletteColor.VERT,
    content: @Composable () -> Unit,
) {
    MyApplicationTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        paletteColor = paletteColor,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    paletteColor: AdtPaletteColor = AdtPaletteColor.VERT,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> buildDarkColorScheme(paletteColor)
        else -> buildLightColorScheme(paletteColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
