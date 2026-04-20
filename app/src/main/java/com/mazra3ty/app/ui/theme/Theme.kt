package com.mazra3ty.app.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimary,
    secondary = GreenPrimaryDark,
    background = Color(0xFF0F172A), // dark background
    surface = Color(0xFF1E293B),

    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,

    error = RedError
)
private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = GreenPrimaryDark,

    background = BackgroundLight,
    surface = SurfaceWhite,

    onPrimary = Color.White,
    onSecondary = Color.White,

    onBackground = TextPrimary,
    onSurface = TextPrimary,

    error = RedError
)

///bakir//
private val LightColors = lightColorScheme(
    primary        = GreenPrimary,
    secondary      = GreenButton,
    background     = Background,
    surface        = White,
    onPrimary      = White,
    onBackground   = TextDark,
    onSurface      = TextDark,
)

@Composable
fun Mazra3tyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content     = content
    )
}

///////////////

@Composable
fun Mazra3tyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(),
        content = content
    )
}