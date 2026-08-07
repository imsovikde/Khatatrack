package com.example.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KhataTheme

@Composable
fun getKhataLogoVector(): ImageVector {
    val isDark = KhataTheme.colors.isDark
    // The contrast color adapts: White in Dark Mode, #050505 in Light Mode
    val contrastColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF050505)
    val accentColor = Color(0xFF28D7B9)

    return remember(isDark) {
        ImageVector.Builder(
            name = "KhataLogo",
            defaultWidth = 24.dp,
            defaultHeight = 28.dp,
            viewportWidth = 116.4f,
            viewportHeight = 137f
        ).apply {
            // Main body
            path(
                fill = SolidColor(contrastColor),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(108.9f, 115.5f)
                lineToRelative(-0.8f, -1.9f)
                curveToRelative(-1.9f, -10.5f, -10.1f, -22.8f, -26.2f, -22.9f)
                horizontalLineToRelative(-9.9f)
                curveToRelative(-4.2f, 0f, -8.6f, -1.7f, -11.8f, -5f)
                lineToRelative(-17f, -18.3f)
                lineToRelative(-8.8f, 8.9f)
                curveToRelative(-2f, 1.9f, -3.1f, 5.5f, -0.3f, 8.6f)
                lineToRelative(15f, 15.4f)
                curveToRelative(5f, 5.1f, 11.5f, 8.9f, 22.7f, 8.9f)
                horizontalLineToRelative(9.2f)
                curveToRelative(5.6f, -0.2f, 9.6f, 2.8f, 11.8f, 6.2f)
                lineToRelative(1.7f, 4.3f)
                lineToRelative(-0.2f, 0.4f)
                lineToRelative(-7.9f, 4.6f)
                lineToRelative(22.1f, 10.5f)
                lineToRelative(6.3f, -22.4f)
                lineToRelative(-5.9f, 2.7f)
                close()
            }
            // Vertical stem and lower leg
            path(
                fill = SolidColor(contrastColor),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(26.6f, 80.3f)
                lineToRelative(-8.3f, -9.3f)
                curveToRelative(-2.5f, -3f, -2.8f, -8.5f, 0.6f, -12f)
                lineToRelative(7.7f, -8.4f)
                verticalLineToRelative(-45.6f)
                curveToRelative(0f, -1.5f, -1.2f, -2.7f, -2.7f, -2.7f)
                horizontalLineToRelative(-9.7f)
                curveToRelative(-6f, -0.1f, -12.4f, 5.2f, -12.4f, 11.6f)
                verticalLineToRelative(101.8f)
                curveToRelative(0f, 6.3f, 4.8f, 11.4f, 11.9f, 11.3f)
                horizontalLineToRelative(10f)
                curveToRelative(1.7f, 0f, 2.9f, -1.2f, 2.9f, -2.7f)
                verticalLineToRelative(-44f)
                close()
            }
            // Upper leg
            path(
                fill = SolidColor(accentColor),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(83.6f, 8.2f)
                lineToRelative(8.2f, 8f)
                lineToRelative(-0.2f, 0.4f)
                curveToRelative(-2.4f, 2.9f, -5.9f, 5.9f, -11.3f, 5.9f)
                horizontalLineToRelative(-9.3f)
                curveToRelative(-6.8f, 0f, -15.6f, 1.1f, -22.3f, 7.9f)
                lineToRelative(-27.2f, 29.6f)
                curveToRelative(-2.2f, 2f, -3f, 6.4f, -0.4f, 9.8f)
                lineToRelative(9f, 10.5f)
                curveToRelative(0.1f, -2.1f, 0.8f, -3.8f, 2.9f, -5.8f)
                lineToRelative(27.2f, -28.9f)
                curveToRelative(2.7f, -2.6f, 6.7f, -5.2f, 12f, -5.2f)
                horizontalLineToRelative(9.4f)
                curveToRelative(7.5f, 0f, 15.3f, -1.1f, 22.2f, -7.7f)
                lineToRelative(1.6f, -1.8f)
                lineToRelative(7.8f, 7.1f)
                verticalLineToRelative(-29.8f)
                horizontalLineToRelative(-29.6f)
                close()
            }
        }.build()
    }
}
