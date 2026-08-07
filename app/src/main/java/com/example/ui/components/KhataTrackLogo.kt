package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun KhataTrackLogo(
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val primaryColor = if (isDark) Color.White else Color(0xFF0E0D0F)
    val accentColor = Color(0xFF26DBC1)

    val width = height * (150f / 172.4f)

    val leftStemPathData = "M48.2,14 v142 c0,6.5 -5.3,13 -13.2,13 h-18.6 c-7.1,0 -13.6,-4.9 -13.6,-13 v-23.5 h12 c2.3,0 4.3,-2 4.3,-4.4 v-0.1 c0,-2.3 -2,-4.4 -4.3,-4.4 h-12 l-0.1,-32.6 h12 c2.3,0 4.7,-1.6 4.7,-4.4 s-2.2,-4.8 -4.4,-4.8 h-12.2 l0.1,-33.5 h12.1 c2.3,0.2 4.4,-1.8 4.4,-3.9 c0,-2.5 -2.4,-4.4 -4,-4.4 h-12.7 l-0.1,-26 c0,-5 3.7,-10.8 10.7,-11 h23.8 c7,0 11.1,5.4 11.1,11 z"
    val upperBranchPathData = "M53.1,97 s-0.1,-23.9 0,-24.5 c0.3,-8.4 7.1,-14.6 19.3,-18.6 c4.1,-1.3 9.1,-3.5 13.6,-7.7 l39.7,-39.7 c11,-10.3 21.6,-1.4 21.6,9.5 c0,3 -1.2,6.2 -3.1,8.1 l-37.8,38.5 c-8.3,7.4 -21.9,11 -32.3,15.6 c-17.6,7.6 -20.5,15.5 -21,18.8 z"
    val lowerBranchPathData = "M76.8,83.2 c-0.6,2.8 -1.8,11.7 13.5,18.8 c12.1,6.5 13.7,7 16.4,9.6 l37.7,38.7 c2,2.1 2.9,4.7 2.9,7.8 c0,10.8 -14.2,15.2 -22.1,8.6 l-38.6,-38.2 c-4.1,-3.8 -8.2,-6.4 -12.7,-8.5 c-5.3,-2.3 -16.5,-6.8 -16.5,-17.8 c0.2,-6.1 6.1,-12.2 19.4,-19 z"

    val leftStemPath = PathParser().parsePathString(leftStemPathData).toPath()
    val upperBranchPath = PathParser().parsePathString(upperBranchPathData).toPath()
    val lowerBranchPath = PathParser().parsePathString(lowerBranchPathData).toPath()

    Canvas(modifier = modifier.size(width = width, height = height)) {
        val scaleX = size.width / 150f
        val scaleY = size.height / 172.4f
        
        drawContext.canvas.save()
        drawContext.canvas.scale(scaleX, scaleY)
        
        drawPath(path = leftStemPath, brush = SolidColor(primaryColor))
        drawPath(path = upperBranchPath, brush = SolidColor(primaryColor))
        drawPath(path = lowerBranchPath, brush = SolidColor(accentColor))
        
        drawContext.canvas.restore()
    }
}
