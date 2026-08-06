package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Part B.3 Spacing Scale
object KhataSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val space2xl = 48.dp

    val screenPadding = 16.dp
    val cardPadding = 16.dp
    val sectionGap = 24.dp
}

// Part B.4 Shape Tokens
object KhataShapes {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(16.dp)
    val sheetTop = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val lg = RoundedCornerShape(28.dp)
    val full = RoundedCornerShape(50)
}

// Part B.4 Elevation Tokens
object KhataElevation {
    val flat = 0.dp
    val restingCard = 2.dp
    val activeSheetOrFab = 6.dp
}
