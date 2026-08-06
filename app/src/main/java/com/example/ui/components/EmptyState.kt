package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle

@Composable
fun EmptyState(
    message: String,
    icon: ImageVector = Icons.Outlined.ReceiptLong,
    subMessage: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(KhataTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textDisabled,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(KhataTheme.spacing.md))
        Text(
            text = message,
            style = TitleStyle,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        if (subMessage != null) {
            Spacer(modifier = Modifier.height(KhataTheme.spacing.xs))
            Text(
                text = subMessage,
                style = CaptionStyle,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))
            PrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(0.7f)
            )
        }
    }
}
