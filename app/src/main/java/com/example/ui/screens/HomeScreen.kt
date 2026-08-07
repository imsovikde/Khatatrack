package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactWithBalance
import com.example.data.repository.SummaryTotals
import com.example.ui.components.ContactCard
import com.example.ui.components.EmptyState
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SemanticChip
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.DisplayStyle
import com.example.ui.theme.HeadlineStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.LabelStyle
import com.example.ui.theme.TitleStyle
import com.example.ui.viewmodel.FilterOption
import com.example.util.CurrencyFormatter

import com.example.data.model.Transaction
import com.example.ui.components.TransactionRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    summaryTotals: SummaryTotals,
    contacts: List<ContactWithBalance>,
    quickEntries: List<Transaction>,
    currentFilter: FilterOption,
    onFilterSelect: (FilterOption) -> Unit,
    onContactClick: (Long) -> Unit,
    onAddContactClick: () -> Unit,
    onQuickVoiceClick: () -> Unit = {},
    onQuickLedgerEntryClick: () -> Unit = {},
    onSearchClick: () -> Unit,
    onNavigateToIncomeExpense: (() -> Unit)? = null,
    onTogglePin: ((Long, Boolean, String) -> Unit)? = null,
    onDeleteContact: ((Long, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    var actionSheetContact by remember { mutableStateOf<ContactWithBalance?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val pinnedContacts = remember(contacts) { contacts.filter { it.contact.isPinned } }
    val otherContacts = remember(contacts) { contacts.filter { !it.contact.isPinned } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.example.ui.components.KhataTrackLogo(height = 32.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KhataTrack",
                            style = TitleStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onQuickVoiceClick,
                        modifier = Modifier.testTag("quick_voice_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Quick Voice Entry",
                            tint = colors.textPrimary
                        )
                    }
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.testTag("search_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgCanvas)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContactClick,
                shape = KhataTheme.shapes.lg,
                containerColor = colors.textPrimary,
                contentColor = if (colors.isDark) colors.bgCanvas else colors.bgSurface,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_contact_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Contact",
                    modifier = Modifier.size(28.dp)
                )
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
            // SUMMARY CARD
            Card(
                shape = KhataTheme.shapes.md,
                colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = KhataTheme.elevation.restingCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md, vertical = KhataTheme.spacing.xs)
                    .testTag("summary_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KhataTheme.spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NET BALANCE",
                        style = CaptionStyle,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val net = summaryTotals.netBalance
                    val netColor = when {
                        net > 0 -> colors.credit
                        net < 0 -> colors.debit
                        else -> colors.textPrimary
                    }
                    val netSign = if (net > 0) "+ " else if (net < 0) "- " else ""
                    Text(
                        text = "$netSign${CurrencyFormatter.formatRupee(Math.abs(net))}",
                        style = DisplayStyle,
                        color = netColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(KhataTheme.spacing.md))
                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "You'll Get",
                                style = CaptionStyle,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.formatRupee(summaryTotals.totalGet),
                                style = HeadlineStyle.copy(fontSize = 18.sp),
                                color = colors.credit,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                                .background(colors.divider)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "You'll Pay",
                                style = CaptionStyle,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.formatRupee(summaryTotals.totalPay),
                                style = HeadlineStyle.copy(fontSize = 18.sp),
                                color = colors.debit,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // INCOME & EXPENSE TRACKER LINK CARD
            Card(
                shape = KhataTheme.shapes.md,
                colors = CardDefaults.cardColors(containerColor = colors.bgSurfaceElevated),
                elevation = CardDefaults.cardElevation(defaultElevation = KhataTheme.elevation.restingCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md, vertical = KhataTheme.spacing.xs)
                    .clickable { onNavigateToIncomeExpense?.invoke() }
                    .testTag("income_expense_tracker_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = KhataTheme.spacing.md, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Income & Expense Tracker →",
                            style = BodyStyle.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Personal money in/out without contact ledgers",
                            style = CaptionStyle,
                            color = colors.textSecondary
                        )
                    }
                    Text(
                        text = "OPEN",
                        style = LabelStyle.copy(fontWeight = FontWeight.Bold),
                        color = colors.credit
                    )
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.xs))

            // QUICK ENTRY ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onQuickLedgerEntryClick,
                    shape = KhataTheme.shapes.md,
                    modifier = Modifier.weight(1f).testTag("quick_ledger_entry_button")
                ) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Ledger Entry", style = LabelStyle.copy(fontWeight = FontWeight.SemiBold))
                }
                OutlinedButton(
                    onClick = onQuickVoiceClick,
                    shape = KhataTheme.shapes.md,
                    modifier = Modifier.weight(1f).testTag("quick_voice_entry_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Voice Entry", style = LabelStyle.copy(fontWeight = FontWeight.SemiBold))
                }
            }

            // FRICTIONLESS VOICE ENTRY BANNER
            Card(
                shape = KhataTheme.shapes.md,
                colors = CardDefaults.cardColors(containerColor = colors.bgSurfaceElevated),
                elevation = CardDefaults.cardElevation(defaultElevation = KhataTheme.elevation.restingCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 2.dp)
                    .clickable { onQuickVoiceClick() }
                    .testTag("quick_voice_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.textPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (colors.isDark) colors.bgCanvas else colors.bgSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Frictionless Voice Entry",
                                style = TitleStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Speak: 'Gave Rahul 500 via UPI'",
                                style = CaptionStyle,
                                color = colors.textSecondary
                            )
                        }
                    }
                    Button(
                        onClick = onQuickVoiceClick,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.textPrimary,
                            contentColor = if (colors.isDark) colors.bgCanvas else colors.bgSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("speak_now_button")
                    ) {
                        Text(
                            text = "Speak",
                            style = LabelStyle.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.sm))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SemanticChip(
                    text = "All",
                    isSelected = currentFilter == FilterOption.ALL,
                    onClick = { onFilterSelect(FilterOption.ALL) },
                    testTag = "filter_all"
                )
                SemanticChip(
                    text = "You'll Get",
                    isSelected = currentFilter == FilterOption.YOU_GET,
                    activeSurfaceColor = colors.creditSurface,
                    activeTextColor = colors.credit,
                    onClick = { onFilterSelect(FilterOption.YOU_GET) },
                    testTag = "filter_you_get"
                )
                SemanticChip(
                    text = "You'll Pay",
                    isSelected = currentFilter == FilterOption.YOU_PAY,
                    activeSurfaceColor = colors.debitSurface,
                    activeTextColor = colors.debit,
                    onClick = { onFilterSelect(FilterOption.YOU_PAY) },
                    testTag = "filter_you_pay"
                )
                SemanticChip(
                    text = "Quick Entries",
                    isSelected = currentFilter == FilterOption.QUICK_ENTRIES,
                    onClick = { onFilterSelect(FilterOption.QUICK_ENTRIES) },
                    testTag = "filter_quick_entries"
                )
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.sm))

            if (currentFilter == FilterOption.QUICK_ENTRIES) {
                if (quickEntries.isEmpty()) {
                    EmptyState(
                        message = "No Quick Entries yet",
                        subMessage = "Add standalone transactions that don't belong to a specific contact.",
                        icon = Icons.Default.People,
                        actionLabel = "Add Quick Entry",
                        onAction = {},
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(quickEntries, key = { "quick_${it.id}" }) { item ->
                            TransactionRow(
                                transaction = item,
                                highlightQuery = "",
                                onClick = {}
                            )
                        }
                    }
                }
            } else {
                // Contact List with Pinned Section Header
                if (contacts.isEmpty()) {
                    EmptyState(
                        message = if (currentFilter == FilterOption.ALL) "No contacts added yet" else "No matching balances",
                        subMessage = "Tap the '+' button below to create your first ledger record.",
                        icon = Icons.Default.People,
                        actionLabel = "Add Contact",
                        onAction = onAddContactClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (pinnedContacts.isNotEmpty()) {
                        item {
                            Text(
                                text = "PINNED CONTACTS",
                                style = CaptionStyle.copy(fontWeight = FontWeight.Bold),
                                color = colors.credit,
                                modifier = Modifier.padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
                            )
                        }
                        items(pinnedContacts, key = { "pinned_${it.contact.id}" }) { item ->
                            ContactCard(
                                contactWithBalance = item,
                                onClick = { onContactClick(item.contact.id) },
                                onLongClick = { actionSheetContact = item },
                                onPinClick = {
                                    onTogglePin?.invoke(item.contact.id, item.contact.isPinned, item.contact.name)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ALL CONTACTS",
                                style = CaptionStyle.copy(fontWeight = FontWeight.Bold),
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
                            )
                        }
                        items(otherContacts, key = { "all_${it.contact.id}" }) { item ->
                            ContactCard(
                                contactWithBalance = item,
                                onClick = { onContactClick(item.contact.id) },
                                onLongClick = { actionSheetContact = item },
                                onPinClick = {
                                    onTogglePin?.invoke(item.contact.id, item.contact.isPinned, item.contact.name)
                                }
                            )
                        }
                    } else {
                        items(contacts, key = { it.contact.id }) { item ->
                            ContactCard(
                                contactWithBalance = item,
                                onClick = { onContactClick(item.contact.id) },
                                onLongClick = { actionSheetContact = item },
                                onPinClick = {
                                    onTogglePin?.invoke(item.contact.id, item.contact.isPinned, item.contact.name)
                                }
                            )
                        }
                    }
                }
                }
            }
        }
    }

    // Long Press Action Bottom Sheet
    val currentSheetContact = actionSheetContact
    if (currentSheetContact != null) {
        ModalBottomSheet(
            onDismissRequest = { actionSheetContact = null },
            sheetState = sheetState,
            containerColor = colors.bgSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 8.dp)
            ) {
                Text(
                    text = currentSheetContact.contact.name,
                    style = TitleStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = currentSheetContact.contact.mobileNumber ?: "No Phone",
                    style = CaptionStyle,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                // Pin / Unpin Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val c = currentSheetContact.contact
                            actionSheetContact = null
                            onTogglePin?.invoke(c.id, c.isPinned, c.name)
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentSheetContact.contact.isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                        contentDescription = null,
                        tint = colors.credit,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (currentSheetContact.contact.isPinned) "Unpin Contact" else "Pin Contact to Top",
                        style = BodyStyle,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // View Ledger Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val id = currentSheetContact.contact.id
                            actionSheetContact = null
                            onContactClick(id)
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Open Ledger",
                        style = BodyStyle,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Delete Contact Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val c = currentSheetContact.contact
                            actionSheetContact = null
                            onDeleteContact?.invoke(c.id, c.name)
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = colors.debit,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Move to Trash",
                        style = BodyStyle,
                        color = colors.debit,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
