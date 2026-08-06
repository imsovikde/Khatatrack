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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactWithBalance
import com.example.data.model.Reminder
import com.example.ui.components.EmptyState
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.LabelStyle
import com.example.ui.theme.TitleStyle
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.ReminderUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    reminders: List<Reminder>,
    contactsWithBalances: List<ContactWithBalance>,
    onContactClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    val context = LocalContext.current

    // Contact lookup map
    val contactMap = contactsWithBalances.associateBy { it.contact.id }

    // Filter contacts that have positive balance (You'll Get money) or pending reminders
    val dueContacts = contactsWithBalances.filter { it.netBalance > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Collection Reminders",
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
        if (dueContacts.isEmpty() && reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                // Empty state per Part D.5: "You're all caught up" with a single checkmark glyph, no accent color
                EmptyState(
                    message = "You're all caught up",
                    subMessage = "No pending payment collections or overdue reminders.",
                    icon = Icons.Default.CheckCircleOutline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                item {
                    Text(
                        text = "PENDING COLLECTIONS",
                        style = CaptionStyle,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = KhataTheme.spacing.md, vertical = KhataTheme.spacing.sm)
                    )
                }

                items(dueContacts, key = { it.contact.id }) { item ->
                    val contact = item.contact
                    val amount = item.netBalance
                    val now = System.currentTimeMillis()
                    val isOverdue = item.lastActivityTime + (7 * 86400000L) < now

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.bgSurface),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left accent bar (2dp) for overdue items per Part D.5
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(64.dp)
                                .background(if (isOverdue) colors.debit else colors.credit)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = KhataTheme.spacing.md, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    style = TitleStyle,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isOverdue) "Overdue • Settle pending" else "Due soon",
                                    style = CaptionStyle,
                                    color = if (isOverdue) colors.debit else colors.textSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyFormatter.formatRupee(amount),
                                    style = TitleStyle,
                                    color = colors.credit,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                TextButton(
                                    onClick = {
                                        val msg = ReminderUtils.createReminderMessage(contact.name, amount, null)
                                        ReminderUtils.shareViaWhatsApp(context, contact.mobileNumber, msg)
                                    }
                                ) {
                                    Text(
                                        text = "Remind",
                                        style = LabelStyle,
                                        color = colors.credit
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                }
            }
        }
    }
}
