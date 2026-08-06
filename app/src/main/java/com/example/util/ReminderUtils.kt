package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object ReminderUtils {

    /**
     * Requirement E.2:
     * Send Reminder opens native SMS/WhatsApp share intent pre-filled with:
     * "Hi {name}, this is a reminder that ₹{amount} is due since {date}. Please settle at your convenience. — via KhataTrack."
     */
    fun createReminderMessage(contactName: String, amount: Double, dueDateTimestamp: Long?): String {
        val formattedAmount = CurrencyFormatter.formatRupee(amount, showSymbol = false)
        val dateStr = if (dueDateTimestamp != null) {
            DateTimeUtils.formatDate(dueDateTimestamp)
        } else {
            "recently"
        }
        return "Hi $contactName, this is a reminder that ₹$formattedAmount is due since $dateStr. Please settle at your convenience. — via KhataTrack."
    }

    fun shareViaNative(context: Context, message: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Send Payment Reminder")
        context.startActivity(shareIntent)
    }

    fun shareViaWhatsApp(context: Context, phoneNumber: String?, message: String) {
        val cleanNumber = phoneNumber?.replace(Regex("[^0-9+]"), "") ?: ""
        val uri = if (cleanNumber.isNotEmpty()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareViaNative(context, message)
        }
    }

    fun makePhoneCall(context: Context, phoneNumber: String?) {
        if (phoneNumber.isNull_or_blank()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        context.startActivity(intent)
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
