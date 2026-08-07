package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KhataTheme
import com.example.util.BackupSummary
import com.example.util.BackupUtils
import com.example.util.CurrencyManager
import com.example.util.DateTimeUtils
import com.example.ui.viewmodel.KhataViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.example.util.FirebaseAuthSyncManager
import com.example.util.UserSyncState
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudDone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupDataScreen(
    viewModel: KhataViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = KhataTheme.colors
    val scope = rememberCoroutineScope()

    var isEncryptionEnabled by remember { mutableStateOf(CurrencyManager.isEncryptionEnabled(context)) }
    var syncState by remember { mutableStateOf(FirebaseAuthSyncManager.getSyncState(context)) }
    var isSyncing by remember { mutableStateOf(false) }

    var importSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }
    var showReplaceConfirmation by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (summary, error) = BackupUtils.parseAndValidateBackup(context, uri)
            if (error != null) {
                importErrorMessage = error
            } else {
                importSummary = summary
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Backup & Data",
                        style = KhataTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Backup and restore your full KhataTrack offline database safely.",
                style = KhataTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Row 1: Full Backup Export
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            val contacts = viewModel.repository.allContacts.first()
                            val txs = viewModel.repository.allTransactions.first()
                            val cats = viewModel.repository.allCategories.first()
                            val pms = viewModel.repository.allPaymentModes.first()
                            val traces = viewModel.repository.allTraces.first()
                            val rems = viewModel.repository.pendingReminders.first()
                            val ies = viewModel.repository.allIncomeExpenseEntries.first()

                            val jsonString = BackupUtils.generateBackupJson(
                                contacts = contacts,
                                transactions = txs,
                                categories = cats,
                                paymentModes = pms,
                                traceLogs = traces,
                                reminders = rems,
                                incomeExpenseEntries = ies,
                                encrypt = isEncryptionEnabled
                            )

                            BackupUtils.exportBackupFile(context, jsonString, contacts, txs)
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Full Backup Export (Secure JSON)",
                            style = KhataTheme.typography.bodyLarge,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Export encrypted .ktb.json file with checksum verification.",
                            style = KhataTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = colors.textDisabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Restore / Import
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        filePickerLauncher.launch("*/*")
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        tint = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Restore / Import (Secure JSON)",
                            style = KhataTheme.typography.bodyLarge,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select a valid .ktb.json backup file to import records.",
                            style = KhataTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = colors.textDisabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Optional Encryption Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Encrypt Backup File",
                            style = KhataTheme.typography.bodyLarge,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AES encryption at rest for maximum local privacy.",
                            style = KhataTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    Switch(
                        checked = isEncryptionEnabled,
                        onCheckedChange = {
                            isEncryptionEnabled = it
                            CurrencyManager.setEncryptionEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.surface,
                            checkedTrackColor = colors.textPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 4: Firebase Auth & Firestore Sync Card
            var showAuthDialog by remember { mutableStateOf(false) }
            var emailInput by remember { mutableStateOf("") }
            var passwordInput by remember { mutableStateOf("") }

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Firebase Cloud Data Sync",
                                style = KhataTheme.typography.bodyLarge,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Account: ${syncState.email ?: "Local Guest"} (${syncState.syncStatus})",
                                style = KhataTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAuthDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (syncState.isSignedIn) "Account Settings" else "Sign In / Auth")
                        }

                        Button(
                            onClick = {
                                isSyncing = true
                                scope.launch {
                                    val res = FirebaseAuthSyncManager.fullSyncWithFirestore(context, viewModel.repository)
                                    isSyncing = false
                                    syncState = FirebaseAuthSyncManager.getSyncState(context)
                                    res.fold(
                                        onSuccess = { msg -> viewModel.showSnackbar(msg) },
                                        onFailure = { err -> viewModel.showSnackbar("Firebase Sync Error: ${err.localizedMessage}") }
                                    )
                                }
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = colors.background,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Syncing...")
                            } else {
                                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now")
                            }
                        }
                    }
                }
            }

            if (showAuthDialog) {
                AlertDialog(
                    onDismissRequest = { showAuthDialog = false },
                    title = { Text("Firebase Cloud Account") },
                    text = {
                        Column {
                            Text(
                                text = "Authenticate to sync your Contacts and Transactions across devices automatically.",
                                style = KhataTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    val authRes = if (emailInput.isNotBlank()) {
                                        FirebaseAuthSyncManager.signInWithEmail(context, emailInput.trim(), passwordInput.trim())
                                    } else {
                                        FirebaseAuthSyncManager.signInAnonymously(context)
                                    }
                                    authRes.fold(
                                        onSuccess = {
                                            showAuthDialog = false
                                            syncState = FirebaseAuthSyncManager.getSyncState(context)
                                            // Automatically trigger full sync after auth
                                            val syncRes = FirebaseAuthSyncManager.fullSyncWithFirestore(context, viewModel.repository)
                                            syncRes.fold(
                                                onSuccess = { msg -> viewModel.showSnackbar("Auth Success! $msg") },
                                                onFailure = { err -> viewModel.showSnackbar("Auth Success, sync error: ${err.localizedMessage}") }
                                            )
                                        },
                                        onFailure = { err ->
                                            viewModel.showSnackbar("Auth Error: ${err.localizedMessage}")
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("Sign In & Sync")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAuthDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    // Error Dialog
    importErrorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { importErrorMessage = null },
            title = { Text("Import Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { importErrorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Backup Summary Preview Dialog
    importSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { importSummary = null },
            title = { Text("Backup Verified") },
            text = {
                Column {
                    Text(
                        text = "Backup Date: ${DateTimeUtils.formatDate(summary.exportedAt)}",
                        style = KhataTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• ${summary.contactCount} Contacts")
                    Text("• ${summary.transactionCount} Transactions")
                    Text("• ${summary.categoryCount} Categories")
                    Text("• ${summary.paymentModeCount} Payment Modes")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose how you would like to import this backup into your current database:",
                        style = KhataTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            scope.launch {
                                viewModel.repository.restoreBackupData(
                                    contacts = summary.contacts,
                                    transactions = summary.transactions,
                                    categories = summary.categories,
                                    paymentModes = summary.paymentModes,
                                    incomeExpenseEntries = summary.incomeExpenseEntries,
                                    traceLogs = summary.traceLogs,
                                    reminders = summary.reminders,
                                    replaceExisting = false
                                )
                                viewModel.showSnackbar("Backup merged successfully!")
                                importSummary = null
                            }
                        }
                    ) {
                        Text("Merge")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            showReplaceConfirmation = true
                        }
                    ) {
                        Text("Replace All", color = colors.debitRed)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { importSummary = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReplaceConfirmation && importSummary != null) {
        val summary = importSummary!!
        AlertDialog(
            onDismissRequest = { showReplaceConfirmation = false },
            title = { Text("Confirm Replace All?") },
            text = { Text("This will PERMANENTLY erase all existing contacts and transactions before applying this backup. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.repository.restoreBackupData(
                                contacts = summary.contacts,
                                transactions = summary.transactions,
                                categories = summary.categories,
                                paymentModes = summary.paymentModes,
                                traceLogs = summary.traceLogs,
                                reminders = summary.reminders,
                                replaceExisting = true
                            )
                            viewModel.showSnackbar("Database replaced with backup successfully!")
                            showReplaceConfirmation = false
                            importSummary = null
                        }
                    }
                ) {
                    Text("Erase & Replace", color = colors.debitRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
