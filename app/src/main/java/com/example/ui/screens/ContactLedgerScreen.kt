package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.data.model.Transaction
import com.example.ui.components.EmptyState
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.TransactionRow
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.DisplayStyle
import com.example.ui.theme.HeadlineStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.ExportUtils
import com.example.util.ReminderUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactLedgerScreen(
    contact: Contact?,
    transactions: List<Transaction>,
    netBalance: Double,
    onBackClick: () -> Unit,
    onEditContact: (Contact) -> Unit,
    onDeleteContact: (Long) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onOpenAddTransaction: (String) -> Unit, // Transaction.TYPE_YOU_GAVE or Transaction.TYPE_YOU_GOT
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    val context = LocalContext.current

    if (contact == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Loading contact...", style = BodyStyle, color = colors.textSecondary)
        }
        return
    }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var selectedTxForDelete by remember { mutableStateOf<Transaction?>(null) }
    val deleteSheetState = rememberModalBottomSheetState()

    val initials = contact.name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    val (balanceColor, statusQualifier) = when {
        netBalance > 0 -> Pair(colors.credit, "You will get")
        netBalance < 0 -> Pair(colors.debit, "You will pay")
        else -> Pair(colors.textSecondary, "Settled up")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    // Rule: Every screen has exactly one back-navigation affordance (top-left chevron)
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = colors.textPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier.background(colors.bgSurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Statement (CSV)", style = BodyStyle, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = colors.textSecondary) },
                            onClick = {
                                showOverflowMenu = false
                                ExportUtils.exportStatementCsv(context, contact, transactions)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Contact", style = BodyStyle, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = colors.textSecondary) },
                            onClick = {
                                showOverflowMenu = false
                                onEditContact(contact)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Contact", style = BodyStyle, color = colors.debit) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.debit) },
                            onClick = {
                                showOverflowMenu = false
                                onDeleteContact(contact.id)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgSurface)
            )
        },
        bottomBar = {
            // FIXED FOOTER (Persists above the list, elevation/1 separation):
            // Two equal-width buttons — "YOU GAVE ₹" (debit outline) / "YOU GOT ₹" (credit filled)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurfaceElevated)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(thickness = 1.dp, color = colors.divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KhataTheme.spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(KhataTheme.spacing.md)
                ) {
                    SecondaryButton(
                        text = "YOU GAVE ₹",
                        onClick = { onOpenAddTransaction(Transaction.TYPE_YOU_GAVE) },
                        borderColor = colors.debit,
                        textColor = colors.debit,
                        modifier = Modifier.weight(1f),
                        testTag = "you_gave_button"
                    )
                    PrimaryButton(
                        text = "YOU GOT ₹",
                        onClick = { onOpenAddTransaction(Transaction.TYPE_YOU_GOT) },
                        containerColor = colors.credit,
                        contentColor = colors.bgSurface,
                        modifier = Modifier.weight(1f),
                        testTag = "you_got_button"
                    )
                }
            }
        },
        containerColor = colors.bgCanvas,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // HEADER BLOCK (bg-surface, hairline bottom border)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .padding(horizontal = KhataTheme.spacing.md, vertical = KhataTheme.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row 1: Avatar (48dp) + Name + Mobile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.divider),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = TitleStyle.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.width(KhataTheme.spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.name,
                            style = HeadlineStyle.copy(fontSize = 20.sp),
                            color = colors.textPrimary
                        )
                        if (!contact.mobileNumber.isNull_or_blank()) {
                            Text(
                                text = contact.mobileNumber ?: "",
                                style = CaptionStyle,
                                color = colors.textSecondary,
                                modifier = Modifier.clickable {
                                    ReminderUtils.makePhoneCall(context, contact.mobileNumber)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

                // Net Balance directly below, large (type/display), semantic color
                Text(
                    text = CurrencyFormatter.formatRupee(Math.abs(netBalance)),
                    style = DisplayStyle,
                    color = balanceColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusQualifier,
                    style = CaptionStyle,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

                // Icon Row: Call · WhatsApp · Remind · Statement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(
                        onClick = { ReminderUtils.makePhoneCall(context, contact.mobileNumber) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = {
                            val msg = ReminderUtils.createReminderMessage(contact.name, Math.abs(netBalance), null)
                            ReminderUtils.shareViaWhatsApp(context, contact.mobileNumber, msg)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "WhatsApp",
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = {
                            val msg = ReminderUtils.createReminderMessage(contact.name, Math.abs(netBalance), null)
                            ReminderUtils.shareViaNative(context, msg)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Remind",
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = { ExportUtils.exportStatementCsv(context, contact, transactions) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Statement",
                            tint = colors.textSecondary
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            // TRANSACTION TIMELINE
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        message = "No entries yet",
                        subMessage = "Add your first entry for ${contact.name} using the buttons below.",
                        actionLabel = null,
                        onAction = null
                    )
                }
            } else {
                // Group transactions by date
                val grouped = remember(transactions) {
                    transactions.groupBy { DateTimeUtils.formatDateHeader(it.transactionDate) }
                }

                // Compute running balances
                var currentRunning = 0.0
                val runningBalanceMap = remember(transactions) {
                    val map = mutableMapOf<Long, Double>()
                    var acc = 0.0
                    for (tx in transactions) {
                        if (tx.type == Transaction.TYPE_YOU_GOT) {
                            acc += tx.amount
                        } else {
                            acc -= tx.amount
                        }
                        map[tx.id] = acc
                    }
                    map
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    grouped.forEach { (dateHeader, txList) ->
                        // Sticky date separator
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.bgCanvas)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dateHeader,
                                    style = CaptionStyle,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        items(txList, key = { it.id }) { tx ->
                            TransactionRow(
                                transaction = tx,
                                runningBalance = runningBalanceMap[tx.id],
                                onClick = {
                                    // Tap = open delete bottom sheet option
                                    selectedTxForDelete = tx
                                },
                                onLongClick = {
                                    selectedTxForDelete = tx
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Bottom Sheet (keep material consistent per Part D.3)
    if (selectedTxForDelete != null) {
        val tx = selectedTxForDelete!!
        ModalBottomSheet(
            onDismissRequest = { selectedTxForDelete = null },
            sheetState = deleteSheetState,
            containerColor = colors.bgSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(KhataTheme.spacing.lg)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Transaction Options",
                    style = TitleStyle,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(KhataTheme.spacing.sm))
                Text(
                    text = "${tx.type} ₹${CurrencyFormatter.formatRupee(tx.amount, showSymbol = false)} on ${DateTimeUtils.formatDate(tx.transactionDate)}",
                    style = BodyStyle,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

                SecondaryButton(
                    text = "DELETE TRANSACTION",
                    onClick = {
                        onDeleteTransaction(tx)
                        selectedTxForDelete = null
                    },
                    borderColor = colors.debit,
                    textColor = colors.debit,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(KhataTheme.spacing.md))
                PrimaryButton(
                    text = "CANCEL",
                    onClick = { selectedTxForDelete = null },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
