package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle
import com.example.util.CurrencyFormatter
import com.example.util.TrashRetentionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    isAppLockEnabled: Boolean,
    onAppLockToggle: (Boolean) -> Unit,
    retentionDays: Int = 30,
    onRetentionDaysChange: ((Int) -> Unit)? = null,
    onOpenCurrency: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenTraceLog: () -> Unit,
    onOpenCategoryManagement: () -> Unit,
    onOpenBackupData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    val context = LocalContext.current
    val activeSymbol = CurrencyFormatter.getActiveCurrencySymbol()
    var showRetentionDialog by remember { mutableStateOf(false) }

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
            // VISIBLE STATUS & TRANSPARENCY GROUP
            Text(
                text = "VISIBLE STATUS & TRANSPARENCY",
                style = CaptionStyle,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 12.dp)
            ) {
                StatusRow("Offline Status", "Ready ✓ (100% On-Device)", colors)
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow("Voice Engine", "Internal + Guided (Vosk) ✓", colors)
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow("Last Backup", "Database Active ✓", colors)
                Spacer(modifier = Modifier.height(8.dp))
                StatusRow("App Version", "v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})", colors)
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

            // APPEARANCE GROUP
            Text(
                text = "APPEARANCE & REGIONAL",
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
                    .clickable { onOpenCurrency() }
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
                    Text(text = "Global Currency", style = BodyStyle, color = colors.textPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Active ($activeSymbol)", style = BodyStyle, color = colors.textSecondary)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = colors.textDisabled,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

            // RECOVERY & AUDIT
            Text(
                text = "RECOVERY & AUDIT",
                style = CaptionStyle,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .clickable { onOpenTrash() }
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Trash",
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "Trash & Recycle Bin", style = BodyStyle, color = colors.textPrimary)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = colors.textDisabled
                )
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            // RETENTION WINDOW SETTING PREFERENCE (7, 30, 60, 90 DAYS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .clickable { showRetentionDialog = true }
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 14.dp)
                    .testTag("retention_window_preference"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoDelete,
                        contentDescription = "Retention Period",
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Column {
                        Text(text = "Trash Retention Window", style = BodyStyle, color = colors.textPrimary)
                        Text(
                            text = "Auto-purge retention period (retention_window_days)",
                            style = CaptionStyle,
                            color = colors.textSecondary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "$retentionDays Days", style = BodyStyle, color = colors.credit, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = colors.textDisabled,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .clickable { onOpenTraceLog() }
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Trace Log",
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "Activity Trace Log", style = BodyStyle, color = colors.textPrimary)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = colors.textDisabled
                )
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
                    .clickable { onOpenBackupData() }
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
                    .clickable { onOpenCategoryManagement() }
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
                    Text(text = "Manage Categories & Modes", style = BodyStyle, color = colors.textPrimary)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = colors.textDisabled
                )
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

            // INTELLIGENCE & AI PARSING GROUP
            var useAiParsing by remember { mutableStateOf(com.example.util.AiConfigManager.getUseAiParsing(context)) }

            Text(
                text = "INTELLIGENCE & AI PARSING",
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Use AI-Enhanced Voice/Text Parsing", style = BodyStyle, color = colors.textPrimary)
                    Text(
                        text = "Enhance local speech entry with free Gemini BYOK key (off by default)",
                        style = CaptionStyle,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = useAiParsing,
                    onCheckedChange = { checked ->
                        useAiParsing = checked
                        com.example.util.AiConfigManager.setUseAiParsing(context, checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.bgSurface,
                        checkedTrackColor = colors.textPrimary,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.divider
                    ),
                    modifier = Modifier.testTag("use_ai_parsing_switch")
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

            var showDependenciesDialog by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .clickable { showDependenciesDialog = true }
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Dependencies",
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "Dependencies & Licenses", style = BodyStyle, color = colors.textPrimary)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "View",
                    tint = colors.textSecondary
                )
            }
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

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
                Text(text = "v${com.example.BuildConfig.VERSION_NAME}", style = BodyStyle, color = colors.textSecondary)
            }
            
            if (showDependenciesDialog) {
                AlertDialog(
                    onDismissRequest = { showDependenciesDialog = false },
                    title = { Text("Open Source Dependencies", style = TitleStyle, color = colors.textPrimary) },
                    text = {
                        Column {
                            Text("• Room (Data Persistence)", color = colors.textSecondary)
                            Text("• Vosk (Voice Recognition)", color = colors.textSecondary)
                            Text("• Coil (Image Loading)", color = colors.textSecondary)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDependenciesDialog = false }) { Text("Close") }
                    },
                    containerColor = colors.bgSurface
                )
            }
        }
    }

    // RETENTION DAYS SELECTION DIALOG
    if (showRetentionDialog) {
        AlertDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = {
                Text(
                    text = "Select Trash Retention Window",
                    style = KhataTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "Items in Trash older than the selected retention period are automatically purged by the daily CleanupWorker job.",
                        style = KhataTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TrashRetentionManager.AVAILABLE_RETENTION_OPTIONS.forEach { days ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onRetentionDaysChange?.invoke(days)
                                    showRetentionDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$days Days",
                                style = KhataTheme.typography.bodyLarge,
                                fontWeight = if (days == retentionDays) FontWeight.Bold else FontWeight.Normal,
                                color = if (days == retentionDays) colors.textPrimary else colors.textSecondary
                            )
                            if (days == retentionDays) {
                                Text(
                                    text = "Active",
                                    style = KhataTheme.typography.labelSmall,
                                    color = colors.credit,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRetentionDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = colors.bgSurface
        )
    }
}

@Composable
fun StatusRow(label: String, value: String, colors: com.example.ui.theme.KhataColors) {
    Row(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = BodyStyle, color = colors.textPrimary)
        Text(text = value, style = BodyStyle, color = colors.textSecondary)
    }
}
