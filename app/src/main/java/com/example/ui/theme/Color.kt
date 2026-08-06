package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Part B.1 Color Tokens
// Light Mode
val LightBgCanvas = Color(0xFFF7F8FA)
val LightBgSurface = Color(0xFFFFFFFF)
val LightBgSurfaceElevated = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF101418)
val LightTextSecondary = Color(0xFF6B7280)
val LightTextDisabled = Color(0xFFC4C8CD)

val LightCredit = Color(0xFF0C6B58) // You Got (Green)
val LightCreditSurface = Color(0xFFE5F5F0)

val LightDebit = Color(0xFFD64545) // You Gave (Red)
val LightDebitSurface = Color(0xFFFBE9E9)

val LightDivider = Color(0xFFE5E7EB)
val LightOverlay = Color(0x66000000) // 40%

// Dark Mode
val DarkBgCanvas = Color(0xFF121212)
val DarkBgSurface = Color(0xFF1C1C1E)
val DarkBgSurfaceElevated = Color(0xFF242426)
val DarkTextPrimary = Color(0xFFF2F2F3)
val DarkTextSecondary = Color(0xFF9CA3AF)
val DarkTextDisabled = Color(0xFF4B4B4D)

val DarkCredit = Color(0xFF3ED9B5) // You Got (Green)
val DarkCreditSurface = Color(0xFF0E2A24)

val DarkDebit = Color(0xFFFF6B6B) // You Gave (Red)
val DarkDebitSurface = Color(0xFF2E1616)

val DarkDivider = Color(0xFF2C2C2E)
val DarkOverlay = Color(0x99000000) // 60%

@Immutable
data class KhataColors(
    val bgCanvas: Color,
    val bgSurface: Color,
    val bgSurfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val credit: Color,
    val creditSurface: Color,
    val debit: Color,
    val debitSurface: Color,
    val divider: Color,
    val overlay: Color,
    val isDark: Boolean
) {
    val surface: Color get() = bgSurface
    val surfaceBorder: Color get() = divider
    val background: Color get() = bgCanvas
    val creditGreen: Color get() = credit
    val debitRed: Color get() = debit
}

val LocalKhataColors = staticCompositionLocalOf {
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
