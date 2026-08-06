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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.ui.components.SemanticChip
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.DisplayStyle
import com.example.ui.theme.HeadlineStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle
import com.example.ui.viewmodel.FilterOption
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    summaryTotals: SummaryTotals,
    contacts: List<ContactWithBalance>,
    currentFilter: FilterOption,
    onFilterSelect: (FilterOption) -> Unit,
    onContactClick: (Long) -> Unit,
    onAddContactClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "KhataTrack",
                        style = TitleStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                },
                actions = {
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
            // Part C Spec: Single FAB (Home screen only): 56dp circular, "+" icon only (no text label)
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
            // SUMMARY CARD (Part D.1 Layout)
            // Three-column layout: "You'll Get", "You'll Pay", "Net" (visually dominant, display font)
            Card(
                shape = KhataTheme.shapes.md,
                colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = KhataTheme.elevation.restingCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md, vertical = KhataTheme.spacing.sm)
                    .testTag("summary_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KhataTheme.spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Net balance - THE HERO NUMBER
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

                    // Two columns: You'll Get vs You'll Pay
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

            Spacer(modifier = Modifier.height(KhataTheme.spacing.sm))

            // Segmented Filter Row (chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KhataTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SemanticChip(
                    text = "All (${contacts.size})",
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
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.sm))

            // Contact List or Empty State
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
                    items(contacts, key = { it.contact.id }) { item ->
                        ContactCard(
                            contactWithBalance = item,
                            onClick = { onContactClick(item.contact.id) }
                        )
                    }
                }
            }
        }
    }
}
