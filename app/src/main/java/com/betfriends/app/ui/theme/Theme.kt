package com.betfriends.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BetPurple,
    onPrimary = Color.White,
    primaryContainer = BetPurpleDark,
    onPrimaryContainer = Color.White,

    secondary = BetPurpleLight,
    onSecondary = Color.Black,
    secondaryContainer = BetSurfaceHigh,
    onSecondaryContainer = BetWhite,

    tertiary = BetYellow,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF4B4A00),
    onTertiaryContainer = BetYellow,

    background = BetBlack,
    onBackground = BetWhite,

    surface = BetSurface,
    onSurface = BetWhite,
    surfaceVariant = BetSurfaceHigh,
    onSurfaceVariant = BetTextSecondary,

    surfaceDim = BetBlack,
    surfaceBright = Color(0xFF353535),
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = BetSurface,
    surfaceContainer = BetSurfaceHigh,
    surfaceContainerHigh = Color(0xFF292929),
    surfaceContainerHighest = BetSurfaceHighest,

    outline = BetBorder,
    outlineVariant = BetDivider,

    error = BetError,
    onError = Color.White,
    errorContainer = Color(0xFF5D1226),
    onErrorContainer = Color(0xFFFFD9E1),

    inverseSurface = BetWhite,
    inverseOnSurface = BetBlack,
    inversePrimary = BetPurpleDark,

    scrim = Color.Black,
    surfaceTint = BetPurple
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF9220C2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2D5FF),
    onPrimaryContainer = Color(0xFF310046),

    secondary = Color(0xFF76537E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF9D8FF),
    onSecondaryContainer = Color(0xFF2E1037),

    tertiary = Color(0xFF676600),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0EF58),
    onTertiaryContainer = Color(0xFF202000),

    background = BetLightBackground,
    onBackground = BetLightText,

    surface = BetLightSurface,
    onSurface = BetLightText,
    surfaceVariant = BetLightSurfaceHigh,
    onSurfaceVariant = BetLightTextSecondary,

    surfaceDim = Color(0xFFDED8E0),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9F2FB),
    surfaceContainer = Color(0xFFF3ECF5),
    surfaceContainerHigh = Color(0xFFEDE6EF),
    surfaceContainerHighest = Color(0xFFE7E0E9),

    outline = BetLightBorder,
    outlineVariant = Color(0xFFD3C6D4),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    inverseSurface = Color(0xFF362F37),
    inverseOnSurface = Color(0xFFFCEFFB),
    inversePrimary = BetPurpleLight,

    scrim = Color.Black,
    surfaceTint = Color(0xFF9220C2)
)

@Composable
fun BetFriendsTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}