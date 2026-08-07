package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.HorizontalDivider
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Transaction
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle
import androidx.compose.ui.text.withStyle
import com.example.util.CurrencyFormatter

@Composable
fun TransactionRow(
    transaction: Transaction,
    runningBalance: Double? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onPhotoClick: ((String) -> Unit)? = null,
    highlightQuery: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    val isGot = transaction.type == Transaction.TYPE_YOU_GOT

    val amountColor = if (isGot) colors.credit else colors.debit
    val surfaceColor = if (isGot) colors.creditSurface else colors.debitSurface
    val directionIcon = if (isGot) Icons.Default.SouthWest else Icons.Default.NorthEast
    val directionLabel = if (isGot) "YOU GOT" else "YOU GAVE"

    val modeIcon: ImageVector = when (transaction.paymentMode.lowercase()) {
        "cash" -> Icons.Default.Money
        "upi" -> Icons.Default.QrCodeScanner
        "bank transfer" -> Icons.Default.AccountBalance
        "card" -> Icons.Default.CreditCard
        "cheque" -> Icons.Default.ReceiptLong
        else -> Icons.Default.Payment
    }

    val noteDisplay = if (!transaction.note.isNull_or_blank()) {
        transaction.note ?: ""
    } else {
        "${transaction.paymentMode} Entry"
    }

    fun highlightText(text: String, query: String?): androidx.compose.ui.text.AnnotatedString {
        if (query.isNullOrBlank()) return androidx.compose.ui.text.buildAnnotatedString { append(text) }
        val startIndex = text.lowercase().indexOf(query.lowercase())
        if (startIndex == -1) return androidx.compose.ui.text.buildAnnotatedString { append(text) }
        
        return androidx.compose.ui.text.buildAnnotatedString {
            append(text.substring(0, startIndex))
            withStyle(style = androidx.compose.ui.text.SpanStyle(background = colors.credit.copy(alpha = 0.3f), fontWeight = FontWeight.Bold)) {
                append(text.substring(startIndex, startIndex + query.length))
            }
            append(text.substring(startIndex + query.length))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("transaction_row_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KhataTheme.spacing.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Payment Mode Icon Pill
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(surfaceColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription = transaction.paymentMode,
                    tint = amountColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(KhataTheme.spacing.md))

            // Note + time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlightText(noteDisplay, highlightQuery),
                    style = BodyStyle,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.transactionTime.ifEmpty { "Entry" },
                        style = CaptionStyle,
                        color = colors.textSecondary
                    )
                    Text(
                        text = " • ${transaction.paymentMode}",
                        style = CaptionStyle,
                        color = colors.textSecondary
                    )
                    if (!transaction.referenceNumber.isNullOrBlank() && transaction.paymentMode != "Cash") {
                        Text(
                            text = " • Ref: ${transaction.referenceNumber}",
                            style = CaptionStyle,
                            color = colors.textSecondary
                        )
                    }
                    if (!transaction.attachmentPhoto.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Has Attachment",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(KhataTheme.spacing.sm))

            // Signed Amount + running balance
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = directionIcon,
                        contentDescription = directionLabel,
                        tint = amountColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = CurrencyFormatter.formatRupee(transaction.amount),
                        style = TitleStyle,
                        color = amountColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (runningBalance != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Bal: ${CurrencyFormatter.formatRupee(runningBalance)}",
                        style = CaptionStyle,
                        color = colors.textSecondary
                    )
                }
            }
        }

        if (!transaction.attachmentPhoto.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 68.dp, end = 16.dp, bottom = 12.dp)
                    .height(140.dp)
                    .clip(KhataTheme.shapes.sm)
                    .background(colors.divider)
            ) {
                AsyncImage(
                    model = transaction.attachmentPhoto,
                    contentDescription = "Attachment",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onPhotoClick?.invoke(transaction.attachmentPhoto) }
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp),
            thickness = 1.dp,
            color = colors.divider
        )
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
