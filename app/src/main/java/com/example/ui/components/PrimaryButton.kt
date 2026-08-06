package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.LabelStyle

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    testTag: String = "primary_button"
) {
    val colors = KhataTheme.colors
    val bg = containerColor ?: colors.textPrimary
    val fg = contentColor ?: if (colors.isDark) colors.bgCanvas else colors.bgSurface

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = KhataTheme.shapes.lg,
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = fg,
            disabledContainerColor = colors.textDisabled,
            disabledContentColor = colors.textSecondary
        ),
        modifier = modifier
            .height(56.dp)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = LabelStyle
        )
    }
}
