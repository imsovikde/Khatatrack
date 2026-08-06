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
import com.example.ui.components.EmptyState
import com.example.ui.components.SemanticChip
import com.example.ui.components.TransactionRow
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.DisplayStyle
import com.example.ui.theme.HeadlineStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle
import com.example.ui.viewmodel.ReportDateRange
import com.example.util.CurrencyFormatter
import com.example.util.ExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    transactions: List<Transaction>,
    selectedRange: ReportDateRange,
    onRangeSelect: (ReportDateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    val context = LocalContext.current

    // Calculate totals for range
    val (totalGiven, totalGot, net) = remember(transactions) {
        var given = 0.0
        var got = 0.0
        for (tx in transactions) {
            if (tx.type == Transaction.TYPE_YOU_GOT) {
                got += tx.amount
            } else {
                given += tx.amount
            }
        }
        Triple(given, got, got - given)
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

            Spacer(modifier = Modifier.height(KhataTheme.spacing.sm))

            // Custom Compose Bar Chart (Monochrome axis, credit/debit tokens)
            Card(
                shape = KhataTheme.shapes.md,
                colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = KhataTheme.elevation.restingCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KhataTheme.spacing.md)
                ) {
                    Text(
                        text = "CASHFLOW COMPARISON",
                        style = CaptionStyle,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

                    val maxVal = Math.max(totalGiven, totalGot).coerceAtLeast(100.0)
                    val creditColor = colors.credit
                    val debitColor = colors.debit
                    val dividerColor = colors.divider

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Baseline
                        drawLine(
                            color = dividerColor,
                            start = Offset(0f, canvasHeight - 1f),
                            end = Offset(canvasWidth, canvasHeight - 1f),
                            strokeWidth = 2f
                        )

                        val barWidth = canvasWidth * 0.25f
                        val gap = canvasWidth * 0.15f

                        // Given bar
                        val givenHeight = ((totalGiven / maxVal) * (canvasHeight - 20f)).toFloat()
                        drawRoundRect(
                            color = debitColor,
                            topLeft = Offset(gap, canvasHeight - givenHeight),
                            size = Size(barWidth, givenHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        // Got bar
                        val gotHeight = ((totalGot / maxVal) * (canvasHeight - 20f)).toFloat()
                        drawRoundRect(
                            color = creditColor,
                            topLeft = Offset(gap * 2 + barWidth, canvasHeight - gotHeight),
                            size = Size(barWidth, gotHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(text = "You Gave (Debit)", style = CaptionStyle, color = colors.debit)
                        Text(text = "You Got (Credit)", style = CaptionStyle, color = colors.credit)
                    }
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Transaction list
            if (transactions.isEmpty()) {
                EmptyState(
                    message = "No transactions in this period",
                    subMessage = "Try changing the range filter above.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
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
