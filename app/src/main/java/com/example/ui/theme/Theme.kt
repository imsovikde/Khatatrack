package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun KhataTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val khataColors = if (darkTheme) {
        KhataColors(
            bgCanvas = DarkBgCanvas,
            bgSurface = DarkBgSurface,
            bgSurfaceElevated = DarkBgSurfaceElevated,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            textDisabled = DarkTextDisabled,
            credit = DarkCredit,
            creditSurface = DarkCreditSurface,
            debit = DarkDebit,
            debitSurface = DarkDebitSurface,
            divider = DarkDivider,
            overlay = DarkOverlay,
            isDark = true
        )
    } else {
        KhataColors(
            bgCanvas = LightBgCanvas,
            bgSurface = LightBgSurface,
            bgSurfaceElevated = LightBgSurfaceElevated,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textDisabled = LightTextDisabled,
            credit = LightCredit,
            creditSurface = LightCreditSurface,
            debit = LightDebit,
            debitSurface = LightDebitSurface,
            divider = LightDivider,
            overlay = LightOverlay,
            isDark = false
        )
    }

    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            background = DarkBgCanvas,
            surface = DarkBgSurface,
            surfaceContainer = DarkBgSurfaceElevated,
            onBackground = DarkTextPrimary,
            onSurface = DarkTextPrimary,
            primary = DarkTextPrimary,
            onPrimary = DarkBgCanvas,
            secondary = DarkTextSecondary,
            outline = DarkDivider
        )
    } else {
        lightColorScheme(
            background = LightBgCanvas,
            surface = LightBgSurface,
            surfaceContainer = LightBgSurfaceElevated,
            onBackground = LightTextPrimary,
            onSurface = LightTextPrimary,
            primary = LightTextPrimary,
            onPrimary = LightBgSurface,
            secondary = LightTextSecondary,
            outline = LightDivider
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = khataColors.bgCanvas.toArgb()
            window.navigationBarColor = khataColors.bgCanvas.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalKhataColors provides khataColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = KhataTypography,
            content = content
        )
    }
}

object KhataTheme {
    val colors: KhataColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKhataColors.current

    val spacing = KhataSpacing
    val shapes = KhataShapes
    val elevation = KhataElevation
}
