package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CosmicPink,
    secondary = CosmicPurple,
    tertiary = CosmicSlate,
    background = CosmicDark,
    surface = CosmicGrey,
    onPrimary = CosmicDark,
    onSecondary = Color.White,
    onBackground = CosmicSlate,
    onSurface = CosmicSlate,
    outline = Color(0x1AFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = CosmicPurple,
    secondary = CosmicPink,
    tertiary = CosmicDark,
    background = Color(0xFFFBF8F9),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = CosmicDark,
    onBackground = CosmicDark,
    onSurface = CosmicDark,
    outline = Color(0x1A000000)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Matikan dynamic color agar desain Immersive UI tetap konsisten dan artistik!
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
