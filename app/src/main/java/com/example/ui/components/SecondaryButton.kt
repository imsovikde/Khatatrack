package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.LabelStyle

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color? = null,
    textColor: Color? = null,
    testTag: String = "secondary_button"
) {
    val colors = KhataTheme.colors
    val border = borderColor ?: colors.textPrimary
    val textC = textColor ?: colors.textPrimary

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = KhataTheme.shapes.lg,
        border = BorderStroke(1.5.dp, if (enabled) border else colors.textDisabled),
        modifier = modifier
            .height(56.dp)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = LabelStyle,
            color = if (enabled) textC else colors.textSecondary
        )
    }
}
