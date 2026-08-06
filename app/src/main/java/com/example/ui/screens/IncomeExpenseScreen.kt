package com.example.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.data.model.IncomeExpenseEntry
import com.example.ui.components.AddIncomeExpenseBottomSheet
import com.example.ui.components.EmptyState
import com.example.ui.components.PdfPreviewDialog
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle
import com.example.util.CurrencyFormatter
import com.example.util.ExportUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class IncomeExpenseFilter {
    ALL,
    INCOME,
    EXPENSE
}

enum class EntryDatePeriod {
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeExpenseScreen(
    entries: List<IncomeExpenseEntry>,
    onAddEntry: (IncomeExpenseEntry) -> Unit,
    onUpdateEntry: (IncomeExpenseEntry) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = KhataTheme.colors

    var currentFilter by remember { mutableStateOf(IncomeExpenseFilter.ALL) }
    var currentPeriod by remember { mutableStateOf(EntryDatePeriod.THIS_MONTH) }
    var isAddSheetOpen by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<IncomeExpenseEntry?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    var pdfPreviewFile by remember { mutableStateOf<File?>(null) }

    // Period filter logic
    val now = System.currentTimeMillis()
    val periodEntries = remember(entries, currentPeriod) {
        val cal = Calendar.getInstance()
        when (currentPeriod) {
            EntryDatePeriod.THIS_WEEK -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val cutoff = cal.timeInMillis
                entries.filter { it.transactionDate >= cutoff }
            }
            EntryDatePeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                val cutoff = cal.timeInMillis
                entries.filter { it.transactionDate >= cutoff }
            }
            EntryDatePeriod.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                val cutoff = cal.timeInMillis
                entries.filter { it.transactionDate >= cutoff }
            }
            EntryDatePeriod.ALL_TIME -> entries
        }
    }

    val totalIncome = remember(periodEntries) {
        periodEntries.filter { it.type == IncomeExpenseEntry.TYPE_INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(periodEntries) {
        periodEntries.filter { it.type == IncomeExpenseEntry.TYPE_EXPENSE }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    val filteredEntries = remember(periodEntries, currentFilter) {
        when (currentFilter) {
            IncomeExpenseFilter.ALL -> periodEntries
            IncomeExpenseFilter.INCOME -> periodEntries.filter { it.type == IncomeExpenseEntry.TYPE_INCOME }
            IncomeExpenseFilter.EXPENSE -> periodEntries.filter { it.type == IncomeExpenseEntry.TYPE_EXPENSE }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bgCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Income & Expense Tracker",
                        style = TitleStyle,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = colors.textPrimary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(colors.bgSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export CSV", color = colors.textPrimary) },
                                onClick = {
                                    showMenu = false
                                    ExportUtils.exportIncomeExpenseCsv(context, filteredEntries)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export PDF Statement", color = colors.textPrimary) },
                                onClick = {
                                    showMenu = false
                                    val file = ExportUtils.generateIncomeExpensePdf(context, filteredEntries)
                                    pdfPreviewFile = file
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Backup This Module (JSON)", color = colors.textPrimary) },
                                onClick = {
                                    showMenu = false
                                    ExportUtils.exportIncomeExpenseJson(context, filteredEntries)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEntry = null
                    isAddSheetOpen = true
                },
                containerColor = colors.textPrimary,
                contentColor = colors.bgSurface,
                shape = CircleShape,
                modifier = Modifier.testTag("add_income_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Period Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EntryDatePeriod.values().forEach { period ->
                    val isSel = currentPeriod == period
                    val label = when (period) {
                        EntryDatePeriod.THIS_WEEK -> "This Week"
                        EntryDatePeriod.THIS_MONTH -> "This Month"
                        EntryDatePeriod.THIS_YEAR -> "This Year"
                        EntryDatePeriod.ALL_TIME -> "All Time"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) colors.textPrimary else colors.bgSurface)
                            .clickable { currentPeriod = period }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = CaptionStyle,
                            color = if (isSel) colors.bgSurface else colors.textSecondary
                        )
                    }
                }
            }

            // Summary Card (3 Columns: Income, Expense, Net)
            Card(
                shape = KhataTheme.shapes.md,
                colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = KhataTheme.elevation.restingCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KhataTheme.spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TOTAL INCOME", style = CaptionStyle, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatRupee(totalIncome),
                            style = TitleStyle.copy(fontSize = 15.sp),
                            color = colors.credit
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TOTAL EXPENSE", style = CaptionStyle, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatRupee(totalExpense),
                            style = TitleStyle.copy(fontSize = 15.sp),
                            color = colors.debit
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NET", style = CaptionStyle, color = colors.textSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatRupee(netBalance),
                            style = TitleStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = if (netBalance >= 0) colors.credit else colors.debit
                        )
                    }
                }
            }

            // Type Filter Chips (All / Income / Expense)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IncomeExpenseFilter.values().forEach { filter ->
                    val isSel = currentFilter == filter
                    val label = when (filter) {
                        IncomeExpenseFilter.ALL -> "All"
                        IncomeExpenseFilter.INCOME -> "Income"
                        IncomeExpenseFilter.EXPENSE -> "Expense"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) colors.textPrimary else colors.bgSurface)
                            .clickable { currentFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = CaptionStyle,
                            color = if (isSel) colors.bgSurface else colors.textSecondary
                        )
                    }
                }
            }

            // List of Entries
            if (filteredEntries.isEmpty()) {
                EmptyState(
                    message = "No income or expense logged yet",
                    subMessage = "Tap + to add your first entry for this period.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        bottom = 80.dp,
                        start = KhataTheme.spacing.md,
                        end = KhataTheme.spacing.md,
                        top = 6.dp
                    )
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        IncomeExpenseCard(
                            entry = entry,
                            onClick = {
                                editingEntry = entry
                                isAddSheetOpen = true
                            },
                            onDelete = { onDeleteEntry(entry.id) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Sheet
    AddIncomeExpenseBottomSheet(
        isOpen = isAddSheetOpen,
        onDismiss = { isAddSheetOpen = false },
        onSave = { entry ->
            if (editingEntry != null) {
                onUpdateEntry(entry)
            } else {
                onAddEntry(entry)
            }
        },
        editingEntry = editingEntry
    )
}

@Composable
fun IncomeExpenseCard(
    entry: IncomeExpenseEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = KhataTheme.colors
    val isIncome = entry.type == IncomeExpenseEntry.TYPE_INCOME
    val amountColor = if (isIncome) colors.credit else colors.debit

    Card(
        shape = KhataTheme.shapes.sm,
        colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = KhataTheme.elevation.restingCard),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KhataTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.categoryTag,
                        style = BodyStyle.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.bgCanvas)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.paymentMode,
                            style = CaptionStyle.copy(fontSize = 10.sp),
                            color = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(entry.transactionDate))} ${if (!entry.note.isNull_or_blank_compat()) "· ${entry.note}" else ""}",
                    style = CaptionStyle,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"} ${CurrencyFormatter.formatRupee(entry.amount)}",
                    style = BodyStyle.copy(fontWeight = FontWeight.Bold),
                    color = amountColor
                )
                if (entry.attachmentPhoto != null) {
                    Text("📷 Photo", style = CaptionStyle.copy(fontSize = 10.sp), color = colors.credit)
                }
            }
        }
    }
}

private fun String?.isNull_or_blank_compat(): Boolean = this == null || this.isBlank()
