package com.example.school_bell.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SchoolBellDarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = DeepNavy,
    primaryContainer = TealDark,
    onPrimaryContainer = TealLight,

    secondary = GoldPrimary,
    onSecondary = DeepNavy,
    secondaryContainer = GoldDark,
    onSecondaryContainer = GoldLight,

    tertiary = TealLight,
    onTertiary = DeepNavy,

    background = DeepNavy,
    onBackground = TextPrimary,

    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgCard,
    onSurfaceVariant = TextSecondary,

    surfaceContainer = BgCard,
    surfaceContainerHigh = BgElevated,
    surfaceContainerHighest = NavyLight,

    outline = TextDisabled,
    outlineVariant = NavyLight,

    error = StatusRed,
    onError = Color.White,
)

@Composable
fun SchoolBellTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SchoolBellDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepNavy.toArgb()
            window.navigationBarColor = DeepNavy.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SchoolBellTypography,
        content = content
    )
}

// Keep old alias for any remnant references
@Composable
fun School_bellTheme(content: @Composable () -> Unit) = SchoolBellTheme(content)
