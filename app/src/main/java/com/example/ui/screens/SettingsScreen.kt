package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    isAppLockEnabled: Boolean,
    onAppLockToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = TitleStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgCanvas)
            )
        },
        containerColor = colors.bgCanvas,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = KhataTheme.spacing.md)
        ) {
            // APPEARANCE GROUP
            Text(
                text = "APPEARANCE",
                style = CaptionStyle,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "Dark mode",
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "Dark Mode", style = BodyStyle, color = colors.textPrimary)
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = onDarkModeToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.bgSurface,
                        checkedTrackColor = colors.textPrimary,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.divider
                    ),
                    modifier = Modifier.testTag("dark_mode_switch")
                )
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .clickable {
                        Toast.makeText(context, "Currency set to Indian Rupee (₹)", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Currency",
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "Default Currency", style = BodyStyle, color = colors.textPrimary)
                }
                Text(text = "INR (₹)", style = BodyStyle, color = colors.textSecondary)
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

            // SECURITY GROUP
            Text(
                text = "SECURITY",
                style = CaptionStyle,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "App Lock",
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "Biometric / PIN App Lock", style = BodyStyle, color = colors.textPrimary)
                }
                Switch(
                    checked = isAppLockEnabled,
                    onCheckedChange = onAppLockToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.bgSurface,
                        checkedTrackColor = colors.textPrimary,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.divider
                    ),
                    modifier = Modifier.testTag("app_lock_switch")
                )
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

            // DATA GROUP
            Text(
                text = "DATA & BACKUP",
                style = CaptionStyle,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .clickable {
                        Toast.makeText(context, "Local database backed up to app cache", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = "Backup",
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "Backup & Restore Database", style = BodyStyle, color = colors.textPrimary)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = colors.textDisabled
                )
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .clickable {
                        Toast.makeText(context, "Categories: Friend, Family, Customer, Supplier, Other", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Categories",
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "Manage Payment Categories", style = BodyStyle, color = colors.textPrimary)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = colors.textDisabled
                )
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

            // ABOUT GROUP
            Text(
                text = "ABOUT",
                style = CaptionStyle,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Version",
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "KhataTrack Version", style = BodyStyle, color = colors.textPrimary)
                }
                Text(text = "v1.0.0 (Minimalist)", style = BodyStyle, color = colors.textSecondary)
            }
        }
    }
}
