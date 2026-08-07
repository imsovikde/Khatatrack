package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.components.EmptyState
import com.example.ui.components.SemanticChip
import com.example.ui.components.TransactionRow
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.DisplayStyle
import com.example.ui.theme.HeadlineStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle
import com.example.ui.viewmodel.ReportDateRange
import com.example.util.CurrencyFormatter
import com.example.util.ExportUtils

import com.example.data.model.IncomeExpenseEntry
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip

enum class ReportSource {
    LEDGER,
    INCOME_EXPENSE,
    COMBINED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    transactions: List<Transaction>,
    incomeExpenseEntries: List<IncomeExpenseEntry> = emptyList(),
    selectedRange: ReportDateRange,
    onRangeSelect: (ReportDateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    val context = LocalContext.current

    var selectedSource by remember { mutableStateOf(ReportSource.COMBINED) }

    // Calculate totals based on selectedSource
    val (totalGot, totalGiven, net) = remember(transactions, incomeExpenseEntries, selectedSource) {
        var credit = 0.0
        var debit = 0.0

        if (selectedSource == ReportSource.LEDGER || selectedSource == ReportSource.COMBINED) {
            for (tx in transactions) {
                if (tx.type == Transaction.TYPE_YOU_GOT) credit += tx.amount
                else debit += tx.amount
            }
        }
        if (selectedSource == ReportSource.INCOME_EXPENSE || selectedSource == ReportSource.COMBINED) {
            for (entry in incomeExpenseEntries) {
                if (entry.type == IncomeExpenseEntry.TYPE_INCOME) credit += entry.amount
                else debit += entry.amount
            }
        }
        Triple(credit, debit, credit - debit)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reports & Analytics",
                        style = TitleStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                },
                actions = {
                    IconButton(
                        onClick = { ExportUtils.exportReportsCsv(context, transactions) },
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export CSV",
                            tint = colors.textPrimary
                        )
                    }
                    IconButton(
                        onClick = { ExportUtils.exportReportsCsv(context, transactions) },
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = colors.textPrimary
                        )
                    }
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
        ) {
            // Source Module Selector (Ledger / Income & Expense / Combined)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
                    .background(colors.bgSurface, RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReportSource.values().forEach { source ->
                    val isSel = selectedSource == source
                    val label = when (source) {
                        ReportSource.LEDGER -> "Ledger"
                        ReportSource.INCOME_EXPENSE -> "Income/Exp"
                        ReportSource.COMBINED -> "Combined"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) colors.textPrimary else colors.bgSurface)
                            .clickable { selectedSource = source }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = CaptionStyle.copy(fontWeight = FontWeight.Bold),
                            color = if (isSel) colors.bgCanvas else colors.textSecondary
                        )
                    }
                }
            }

            // Range selector chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md, vertical = KhataTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SemanticChip(
                    text = "This Week",
                    isSelected = selectedRange == ReportDateRange.THIS_WEEK,
                    onClick = { onRangeSelect(ReportDateRange.THIS_WEEK) }
                )
                SemanticChip(
                    text = "This Month",
                    isSelected = selectedRange == ReportDateRange.THIS_MONTH,
                    onClick = { onRangeSelect(ReportDateRange.THIS_MONTH) }
                )
                SemanticChip(
                    text = "This Year",
                    isSelected = selectedRange == ReportDateRange.THIS_YEAR,
                    onClick = { onRangeSelect(ReportDateRange.THIS_YEAR) }
                )
            }

            // Summary Card (3 columns)
            Card(
                shape = KhataTheme.shapes.md,
                colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = KhataTheme.elevation.restingCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md, vertical = KhataTheme.spacing.sm)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KhataTheme.spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Total Given", style = CaptionStyle, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.formatRupee(totalGiven),
                                style = HeadlineStyle.copy(fontSize = 18.sp),
                                color = colors.debit,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                                .background(colors.divider)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Total Got", style = CaptionStyle, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.formatRupee(totalGot),
                                style = HeadlineStyle.copy(fontSize = 18.sp),
                                color = colors.credit,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                                .background(colors.divider)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Net Difference", style = CaptionStyle, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            val netColor = if (net >= 0) colors.credit else colors.debit
                            Text(
                                text = CurrencyFormatter.formatRupee(Math.abs(net)),
                                style = HeadlineStyle.copy(fontSize = 18.sp),
                                color = netColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Payment mode breakdown
            val paymentModeAggregates = remember(transactions) {
                transactions.groupBy { it.paymentMode.ifBlank { "Cash" } }
                    .map { (mode, list) -> mode to list.sumOf { tx -> tx.amount } }
                    .sortedByDescending { it.second }
            }

            // Category breakdown
            val categoryAggregates = remember(transactions) {
                transactions.groupBy { if (it.categoryTag.isBlank()) "General" else it.categoryTag }
                    .map { (tag, list) -> tag to list.sumOf { tx -> tx.amount } }
                    .sortedByDescending { it.second }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                // 1. CASHFLOW & TREND CHART
                item {
                    Card(
                        shape = KhataTheme.shapes.md,
                        colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = KhataTheme.elevation.restingCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(KhataTheme.spacing.md)
                        ) {
                            Text(
                                text = "TREND & CASHFLOW",
                                style = CaptionStyle,
                                color = colors.textSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

                            val creditColor = colors.credit
                            val debitColor = colors.debit
                            val dividerColor = colors.divider

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                val canvasW = size.width
                                val canvasH = size.height

                                // Draw baseline
                                drawLine(
                                    color = dividerColor,
                                    start = Offset(0f, canvasH - 10f),
                                    end = Offset(canvasW, canvasH - 10f),
                                    strokeWidth = 2f
                                )

                                if (transactions.isNotEmpty()) {
                                    val sorted = transactions.sortedBy { it.transactionDate }
                                    val count = sorted.size.coerceAtLeast(2)
                                    val stepX = canvasW / (count - 1)
                                    val maxAmt = sorted.maxOf { it.amount }.coerceAtLeast(1.0)

                                    val creditPath = Path()
                                    val debitPath = Path()

                                    sorted.forEachIndexed { index, tx ->
                                        val x = index * stepX
                                        val y = canvasH - 10f - ((tx.amount / maxAmt) * (canvasH - 30f)).toFloat()

                                        if (tx.type == Transaction.TYPE_YOU_GOT) {
                                            if (creditPath.isEmpty) creditPath.moveTo(x, y) else creditPath.lineTo(x, y)
                                        } else {
                                            if (debitPath.isEmpty) debitPath.moveTo(x, y) else debitPath.lineTo(x, y)
                                        }
                                    }

                                    if (!creditPath.isEmpty) {
                                        drawPath(creditPath, color = creditColor, style = Stroke(width = 4f))
                                    }
                                    if (!debitPath.isEmpty) {
                                        drawPath(debitPath, color = debitColor, style = Stroke(width = 4f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // Accessible Data Table
                            Text("Data Table: Cashflow Summary", style = CaptionStyle.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("You Gave (Debit)", style = BodyStyle, color = colors.debit)
                                Text(CurrencyFormatter.formatRupee(totalGiven), style = BodyStyle, color = colors.debit, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("You Got (Credit)", style = BodyStyle, color = colors.credit)
                                Text(CurrencyFormatter.formatRupee(totalGot), style = BodyStyle, color = colors.credit, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 2. CATEGORY BREAKDOWN (DONUT / PIE CHART VIA Canvas drawArc)
                if (categoryAggregates.isNotEmpty()) {
                    item {
                        Card(
                            shape = KhataTheme.shapes.md,
                            colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = KhataTheme.elevation.restingCard),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(KhataTheme.spacing.md)
                            ) {
                                Text(
                                    text = "CATEGORY DONUT BREAKDOWN",
                                    style = CaptionStyle,
                                    color = colors.textSecondary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

                                val totalCatSum = categoryAggregates.sumOf { it.second }.coerceAtLeast(1.0)
                                val arcColors = listOf(colors.credit, colors.debit, colors.textPrimary, colors.textSecondary)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Canvas(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .padding(8.dp)
                                    ) {
                                        var startAngle = -90f
                                        categoryAggregates.take(4).forEachIndexed { i, (tag, amt) ->
                                            val sweep = ((amt / totalCatSum) * 360f).toFloat()
                                            drawArc(
                                                color = arcColors[i % arcColors.size],
                                                startAngle = startAngle,
                                                sweepAngle = sweep,
                                                useCenter = false,
                                                style = Stroke(width = 24f)
                                            )
                                            startAngle += sweep
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        categoryAggregates.take(4).forEachIndexed { i, (tag, amt) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("#$tag", style = CaptionStyle, color = arcColors[i % arcColors.size], fontWeight = FontWeight.Bold)
                                                Text(CurrencyFormatter.formatRupee(amt), style = CaptionStyle, color = colors.textPrimary)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Data Table: Category Totals", style = CaptionStyle.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                                categoryAggregates.forEach { (tag, amt) ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("#$tag", style = BodyStyle, color = colors.textPrimary)
                                        Text(CurrencyFormatter.formatRupee(amt), style = BodyStyle, color = colors.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. PAYMENT MODE DISTRIBUTION BAR CHART
                if (paymentModeAggregates.isNotEmpty()) {
                    item {
                        Card(
                            shape = KhataTheme.shapes.md,
                            colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = KhataTheme.elevation.restingCard),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = KhataTheme.spacing.md, vertical = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(KhataTheme.spacing.md)
                            ) {
                                Text(
                                    text = "PAYMENT MODE DISTRIBUTION",
                                    style = CaptionStyle,
                                    color = colors.textSecondary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(KhataTheme.spacing.sm))

                                val maxModeAmt = paymentModeAggregates.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
                                val barColor = colors.textPrimary
                                val trackColor = colors.divider

                                paymentModeAggregates.forEach { (mode, amt) ->
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = mode, style = BodyStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = colors.textPrimary)
                                            Text(text = CurrencyFormatter.formatRupee(amt), style = CaptionStyle, color = colors.textSecondary)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                        ) {
                                            val canvasW = size.width
                                            val canvasH = size.height
                                            val barW = ((amt / maxModeAmt) * canvasW).toFloat()

                                            drawRoundRect(
                                                color = trackColor,
                                                size = Size(canvasW, canvasH),
                                                cornerRadius = CornerRadius(4f, 4f)
                                            )
                                            drawRoundRect(
                                                color = barColor,
                                                size = Size(barW, canvasH),
                                                cornerRadius = CornerRadius(4f, 4f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Data Table: Payment Modes", style = CaptionStyle.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                                paymentModeAggregates.forEach { (mode, amt) ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(mode, style = BodyStyle, color = colors.textPrimary)
                                        Text(CurrencyFormatter.formatRupee(amt), style = BodyStyle, color = colors.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. TRANSACTIONS HEADER
                item {
                    Text(
                        text = "PERIOD TRANSACTIONS (${transactions.size})",
                        style = CaptionStyle.copy(fontWeight = FontWeight.Bold),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = KhataTheme.spacing.md, vertical = 8.dp)
                    )
                }

                if (transactions.isEmpty()) {
                    item {
                        EmptyState(
                            message = "No transactions in this period",
                            subMessage = "Try changing the range filter above.",
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionRow(
                            transaction = tx,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}
