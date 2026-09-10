package com.housemind.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors =
    lightColorScheme(
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

private val DarkColors =
    darkColorScheme(
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
        if (darkTheme) {
            DarkColors
        } else {
            LightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
