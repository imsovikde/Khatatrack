package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactWithBalance
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils

@Composable
fun ContactCard(
    contactWithBalance: ContactWithBalance,
    onClick: () -> Unit,
    onCallClick: (() -> Unit)? = null,
    onRemindClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    val contact = contactWithBalance.contact
    val balance = contactWithBalance.netBalance

    val (balanceColor, arrowIcon, captionText) = when {
        balance > 0 -> Triple(colors.credit, Icons.Default.SouthWest, "You'll Get")
        balance < 0 -> Triple(colors.debit, Icons.Default.NorthEast, "You'll Pay")
        else -> Triple(colors.textSecondary, null, "Settled up")
    }

    val initials = contact.name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("contact_card_${contact.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KhataTheme.spacing.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar (40dp circle)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.divider),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = TitleStyle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.width(KhataTheme.spacing.md))

            // Name + last activity
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        style = TitleStyle,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (contact.isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "📌",
                            style = CaptionStyle
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Last: ${DateTimeUtils.formatDateHeader(contactWithBalance.lastActivityTime)}",
                    style = CaptionStyle,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.width(KhataTheme.spacing.sm))

            // Balance & arrow
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = CurrencyFormatter.formatRupee(Math.abs(balance)),
                        style = TitleStyle,
                        color = balanceColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (arrowIcon != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = arrowIcon,
                            contentDescription = captionText,
                            tint = balanceColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = captionText,
                    style = CaptionStyle,
                    color = colors.textSecondary
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp),
            thickness = 1.dp,
            color = colors.divider
        )
    }
}
