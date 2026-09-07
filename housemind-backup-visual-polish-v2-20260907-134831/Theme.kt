package com.housemind.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = HMPrimary,
    onPrimary = HMOnPrimary,
    primaryContainer = HMPrimaryContainer,
    onPrimaryContainer = HMOnPrimaryContainer,
    secondary = HMSecondary,
    onSecondary = HMOnSecondary,
    secondaryContainer = HMSecondaryContainer,
    onSecondaryContainer = HMOnSecondaryContainer,
    tertiary = HMTertiary,
    onTertiary = HMOnTertiary,
    tertiaryContainer = HMTertiaryContainer,
    onTertiaryContainer = HMOnTertiaryContainer,
    error = HMError,
    onError = HMOnError,
    errorContainer = HMErrorContainer,
    onErrorContainer = HMOnErrorContainer,
    background = HMBackground,
    onBackground = HMOnBackground,
    surface = HMSurface,
    onSurface = HMOnSurface,
    surfaceVariant = HMSurfaceVariant,
    onSurfaceVariant = HMOnSurfaceVariant,
    outline = HMOutline,
    outlineVariant = HMOutlineVariant,
    scrim = HMScrim
)

private val DarkColors = darkColorScheme(
    primary = HMPrimaryDark,
    onPrimary = HMOnPrimaryDark,
    primaryContainer = HMPrimaryContainerDark,
    onPrimaryContainer = HMOnPrimaryContainerDark,
    secondary = HMSecondaryDark,
    onSecondary = HMOnSecondaryDark,
    secondaryContainer = HMSecondaryContainerDark,
    onSecondaryContainer = HMOnSecondaryContainerDark,
    tertiary = HMTertiaryDark,
    onTertiary = HMOnTertiaryDark,
    tertiaryContainer = HMTertiaryContainerDark,
    onTertiaryContainer = HMOnTertiaryContainerDark,
    error = HMErrorDark,
    onError = HMOnErrorDark,
    errorContainer = HMErrorContainerDark,
    onErrorContainer = HMOnErrorContainerDark,
    background = HMBackgroundDark,
    onBackground = HMOnBackgroundDark,
    surface = HMSurfaceDark,
    onSurface = HMOnSurfaceDark,
    surfaceVariant = HMSurfaceVariantDark,
    onSurfaceVariant = HMOnSurfaceVariantDark,
    outline = HMOutlineDark,
    outlineVariant = HMOutlineVariantDark,
    scrim = HMScrim
)

@Composable
fun HouseMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) DarkColors else LightColors
            }
            darkTheme -> DarkColors
            else -> LightColors
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
