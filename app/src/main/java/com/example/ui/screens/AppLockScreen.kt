package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.DisplayStyle
import com.example.ui.theme.HeadlineStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle

@Composable
fun AppLockScreen(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pinLength = 4
    val correctPin = "1234" // Default PIN

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgCanvas)
            .padding(KhataTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        com.example.ui.components.KhataTrackLogo(height = 64.dp)

        Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

        Text(
            text = "KhataTrack Locked",
            style = HeadlineStyle,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(KhataTheme.spacing.xs))

        Text(
            text = "Enter Security PIN (Default: 1234)",
            style = CaptionStyle,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(KhataTheme.spacing.xl))

        // PIN Indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until pinLength) {
                val isFilled = i < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isFilled) colors.textPrimary else colors.divider)
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))
            Text(
                text = errorMessage!!,
                style = CaptionStyle,
                color = colors.debit,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(KhataTheme.spacing.xl))

        // Keypad grid
        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("FP", "0", "DEL")
            )

            for (row in keys) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (key in row) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(colors.bgSurface)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "FP") {
                                IconButton(onClick = { onUnlock() }) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric Unlock",
                                        tint = colors.credit
                                    )
                                }
                            } else if (key == "DEL") {
                                IconButton(onClick = {
                                    if (enteredPin.isNotEmpty()) {
                                        enteredPin = enteredPin.dropLast(1)
                                        errorMessage = null
                                    }
                                }) {
                                    Text(
                                        text = "⌫",
                                        style = TitleStyle,
                                        color = colors.textPrimary
                                    )
                                }
                            } else {
                                IconButton(onClick = {
                                    if (enteredPin.length < pinLength) {
                                        enteredPin += key
                                        if (enteredPin.length == pinLength) {
                                            if (enteredPin == correctPin) {
                                                onUnlock()
                                            } else {
                                                errorMessage = "Incorrect PIN. Try 1234."
                                                enteredPin = ""
                                            }
                                        }
                                    }
                                }) {
                                    Text(
                                        text = key,
                                        style = TitleStyle.copy(fontSize = 22.sp),
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
