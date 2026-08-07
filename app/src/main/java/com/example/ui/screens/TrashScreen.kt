package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.data.model.Transaction
import com.example.ui.components.EmptyState
import com.example.ui.theme.KhataTheme
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.ui.viewmodel.KhataViewModel

enum class TrashFilter {
    ALL, CONTACTS, TRANSACTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: KhataViewModel,
    onBack: () -> Unit
) {
    val colors = KhataTheme.colors
    val trashContacts by viewModel.repository.trashContacts.collectAsState(initial = emptyList())
    val trashTxs by viewModel.repository.trashTransactions.collectAsState(initial = emptyList())

    var filter by remember { mutableStateOf(TrashFilter.ALL) }

    var itemToDeleteContact by remember { mutableStateOf<Contact?>(null) }
    var itemToDeleteTx by remember { mutableStateOf<Transaction?>(null) }
    var showRetentionDialog by remember { mutableStateOf(false) }

    val retentionDays = viewModel.getTrashRetentionDays()
    val totalCount = trashContacts.size + trashTxs.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trash ($totalCount)",
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
        ) {
            // Retention Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceBorder.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-cleanup window: $retentionDays Days",
                            style = KhataTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Daily WorkManager job purges items older than $retentionDays days.",
                            style = KhataTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    TextButton(onClick = { showRetentionDialog = true }) {
                        Text("Change", style = KhataTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Segmented Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == TrashFilter.ALL,
                    onClick = { filter = TrashFilter.ALL },
                    label = { Text("All (${trashContacts.size + trashTxs.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.surfaceBorder,
                        selectedLabelColor = colors.textPrimary
                    )
                )
                FilterChip(
                    selected = filter == TrashFilter.CONTACTS,
                    onClick = { filter = TrashFilter.CONTACTS },
                    label = { Text("Contacts (${trashContacts.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.surfaceBorder,
                        selectedLabelColor = colors.textPrimary
                    )
                )
                FilterChip(
                    selected = filter == TrashFilter.TRANSACTIONS,
                    onClick = { filter = TrashFilter.TRANSACTIONS },
                    label = { Text("Transactions (${trashTxs.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.surfaceBorder,
                        selectedLabelColor = colors.textPrimary
                    )
                )
            }

            if ((filter == TrashFilter.ALL && totalCount == 0) ||
                (filter == TrashFilter.CONTACTS && trashContacts.isEmpty()) ||
                (filter == TrashFilter.TRANSACTIONS && trashTxs.isEmpty())
            ) {
                EmptyState(
                    message = "Trash is Empty",
                    subMessage = "Deleted contacts and transactions will appear here for recovery before permanent removal."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filter == TrashFilter.ALL || filter == TrashFilter.CONTACTS) {
                        items(trashContacts, key = { "c_${it.id}" }) { contact ->
                            DesaturatedContactTrashRow(
                                contact = contact,
                                onRestore = { viewModel.restoreContact(contact.id, contact.name) },
                                onDeletePermanently = { itemToDeleteContact = contact }
                            )
                        }
                    }

                    if (filter == TrashFilter.ALL || filter == TrashFilter.TRANSACTIONS) {
                        items(trashTxs, key = { "t_${it.id}" }) { tx ->
                            DesaturatedTxTrashRow(
                                tx = tx,
                                onRestore = { viewModel.restoreTransaction(tx.id, "Transaction") },
                                onDeletePermanently = { itemToDeleteTx = tx }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialogs for permanent delete
    itemToDeleteContact?.let { contact ->
        AlertDialog(
            onDismissRequest = { itemToDeleteContact = null },
            title = { Text("Delete Forever?") },
            text = { Text("This will permanently remove '${contact.name}' and all associated records. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteContactPermanently(contact.id, contact.name)
                        itemToDeleteContact = null
                    }
                ) {
                    Text("Delete Forever", color = colors.debitRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteContact = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    itemToDeleteTx?.let { tx ->
        AlertDialog(
            onDismissRequest = { itemToDeleteTx = null },
            title = { Text("Delete Forever?") },
            text = { Text("This will permanently remove this transaction (${CurrencyFormatter.formatRupee(tx.amount)}). This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransactionPermanently(tx.id, "Transaction")
                        itemToDeleteTx = null
                    }
                ) {
                    Text("Delete Forever", color = colors.debitRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteTx = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRetentionDialog) {
        AlertDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = { Text("Trash Retention Period", style = KhataTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        "Items older than the selected retention period are automatically cleaned up daily by a WorkManager background job.",
                        style = KhataTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    com.example.util.TrashRetentionManager.AVAILABLE_RETENTION_OPTIONS.forEach { days ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setTrashRetentionDays(days)
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
            containerColor = colors.surface
        )
    }
}

@Composable
fun DesaturatedContactTrashRow(
    contact: Contact,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val colors = KhataTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    style = KhataTheme.typography.titleMedium,
                    color = colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = KhataTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Contact • Deleted ${contact.deletedAt?.let { DateTimeUtils.formatDate(it) } ?: ""}",
                    style = KhataTheme.typography.bodyMedium,
                    color = colors.textDisabled
                )
            }
            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Default.RestoreFromTrash,
                    contentDescription = "Restore",
                    tint = colors.textPrimary
                )
            }
            IconButton(onClick = onDeletePermanently) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Delete Forever",
                    tint = colors.debitRed
                )
            }
        }
    }
}

@Composable
fun DesaturatedTxTrashRow(
    tx: Transaction,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val colors = KhataTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (tx.type == Transaction.TYPE_YOU_GOT) "YOU GOT" else "YOU GAVE",
                        style = KhataTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = CurrencyFormatter.formatRupee(tx.amount),
                        style = KhataTheme.typography.bodyLarge,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${tx.paymentMode} ${if (!tx.note.isNullOrEmpty()) "• ${tx.note}" else ""} • Deleted ${tx.deletedAt?.let { DateTimeUtils.formatDate(it) } ?: ""}",
                    style = KhataTheme.typography.bodyMedium,
                    color = colors.textDisabled
                )
            }
            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Default.RestoreFromTrash,
                    contentDescription = "Restore",
                    tint = colors.textPrimary
                )
            }
            IconButton(onClick = onDeletePermanently) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Delete Forever",
                    tint = colors.debitRed
                )
            }
        }
    }
}
