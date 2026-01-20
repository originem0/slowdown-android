package com.sharonZ.slowdown.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary500,
    onPrimary = TextOnPrimary,
    primaryContainer = Primary100,
    onPrimaryContainer = Primary600,

    secondary = Secondary500,
    onSecondary = Neutral900,
    secondaryContainer = Secondary100,
    onSecondaryContainer = Secondary600,

    tertiary = Tertiary500,
    onTertiary = TextOnPrimary,
    tertiaryContainer = Tertiary100,
    onTertiaryContainer = Color(0xFF5D1D12),

    background = Neutral50,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = Neutral100,
    onSurfaceVariant = TextSecondary,
    
    outline = Neutral200,

    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C)
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary500,
    onPrimary = Color.White,
    primaryContainer = Primary600,
    onPrimaryContainer = Primary100,

    secondary = Secondary500,
    onSecondary = Neutral900,
    secondaryContainer = Color(0xFF4D3D18),
    onSecondaryContainer = Secondary100,

    tertiary = Tertiary500,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5D1D12),
    onTertiaryContainer = Tertiary100,

    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = DarkTextSecondary,

    outline = Color(0xFF444444),
    
    error = Error,
    onError = Color.White
)

@Composable
fun SlowDownTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic color to maintain brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
