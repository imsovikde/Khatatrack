package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.LabelStyle

@Composable
fun SemanticChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeSurfaceColor: Color? = null,
    activeTextColor: Color? = null,
    testTag: String = "chip_$text"
) {
    val colors = KhataTheme.colors

    val bg = when {
        isSelected && activeSurfaceColor != null -> activeSurfaceColor
        isSelected -> colors.textPrimary
        else -> colors.divider
    }

    val fg = when {
        isSelected && activeTextColor != null -> activeTextColor
        isSelected -> if (colors.isDark) colors.bgCanvas else colors.bgSurface
        else -> colors.textPrimary
    }

    Box(
        modifier = modifier
            .height(32.dp)
            .clip(KhataTheme.shapes.sm)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = LabelStyle.copy(fontSize = 13.sp),
            color = fg
        )
    }
}
