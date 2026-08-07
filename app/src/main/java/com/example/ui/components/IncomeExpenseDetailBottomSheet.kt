package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.IncomeExpenseEntry
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.HeadlineStyle
import com.example.ui.theme.KhataColors
import com.example.ui.theme.KhataTheme
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeExpenseDetailBottomSheet(
    entry: IncomeExpenseEntry,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = KhataTheme.colors
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isIncome = entry.type == IncomeExpenseEntry.TYPE_INCOME
    val amountColor = if (isIncome) colors.credit else colors.debit
    val typeBgColor = if (isIncome) colors.creditSurface else colors.debitSurface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeBgColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isIncome) "INCOME" else "EXPENSE",
                        style = CaptionStyle.copy(fontWeight = FontWeight.Bold),
                        color = amountColor
                    )
                }
                Text(
                    text = "${if (isIncome) "+" else "-"} ${CurrencyFormatter.formatRupee(entry.amount)}",
                    style = HeadlineStyle.copy(fontWeight = FontWeight.Bold),
                    color = amountColor
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            EntryDetailRow("Category", entry.categoryTag, colors)
            EntryDetailRow("Payment Mode", entry.paymentMode, colors)
            EntryDetailRow(
                label = "Date & Time",
                value = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(Date(entry.transactionDate)),
                colors = colors
            )
            if (!entry.transactionRefId.isNullOrBlank()) {
                EntryDetailRow("Reference #", entry.transactionRefId!!, colors)
            }
            if (!entry.note.isNullOrBlank()) {
                EntryDetailRow("Note", entry.note!!, colors)
            }
            if (entry.attachmentPhoto != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Attachment", style = CaptionStyle, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                AsyncImage(
                    model = entry.attachmentPhoto,
                    contentDescription = "Transaction attachment",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.debit),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = colors.bgSurface)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit", color = colors.bgSurface)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Entry?") },
            text = {
                Text(
                    "Are you sure you want to delete this ${if (isIncome) "income" else "expense"} entry? " +
                        "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Delete", color = colors.debit, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EntryDetailRow(label: String, value: String, colors: KhataColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = CaptionStyle,
            color = colors.textSecondary,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = BodyStyle.copy(fontWeight = FontWeight.Medium),
            color = colors.textPrimary,
            modifier = Modifier.weight(0.6f)
        )
    }
}
