package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = PureWhite,
    primaryContainer = SkySurfaceTint,
    onPrimaryContainer = NavyPrimary,
    secondary = CobaltAccent,
    onSecondary = PureWhite,
    secondaryContainer = SoftSurfaceVariant,
    onSecondaryContainer = SlateInk,
    tertiary = EmeraldAllowed,
    onTertiary = PureWhite,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldAllowed,
    error = CrimsonBlocked,
    onError = PureWhite,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonBlocked,
    background = AlabasterBg,
    onBackground = SlateInk,
    surface = PureWhite,
    onSurface = SlateInk,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = SlateSubtle,
    outlineVariant = HairlineBorder,
)

private val DarkColorScheme = darkColorScheme(
    primary = NavyPrimaryDark,
    onPrimary = MidnightBg,
    primaryContainer = ColorTokens.DarkPrimaryContainer,
    onPrimaryContainer = MidnightText,
    secondary = NavyPrimaryDark,
    onSecondary = MidnightBg,
    secondaryContainer = MidnightSurfaceVariant,
    onSecondaryContainer = MidnightText,
    tertiary = EmeraldDark,
    onTertiary = MidnightBg,
    tertiaryContainer = EmeraldDarkContainer,
    onTertiaryContainer = EmeraldDark,
    error = CrimsonDark,
    onError = MidnightBg,
    errorContainer = CrimsonDarkContainer,
    onErrorContainer = CrimsonDark,
    background = MidnightBg,
    onBackground = MidnightText,
    surface = MidnightSurface,
    onSurface = MidnightText,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = MidnightTextMuted,
    outline = MidnightTextMuted,
    outlineVariant = MidnightBorder,
)

private object ColorTokens {
    val DarkPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF1E3A8A)
}

val TravaNetShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TravaNetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
        typography = TravaNetTypography,
        shapes = TravaNetShapes,
        content = content,
    )
}
