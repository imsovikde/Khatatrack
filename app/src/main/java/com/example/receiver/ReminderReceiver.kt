package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.util.CurrencyFormatter
import com.example.util.ReminderUtils

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminderId", 0L)
        val contactId = intent.getLongExtra("contactId", 0L)
        val contactName = intent.getStringExtra("contactName") ?: "Customer"
        val amount = intent.getDoubleExtra("amount", 0.0)
        val phoneNumber = intent.getStringExtra("phoneNumber")

        val channelId = "khata_reminders_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "KhataTrack Payment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for due payment collections and ledger reminders"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action 1: View Ledger Deep-Link
        val ledgerIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("contactId", contactId)
            putExtra("openLedger", true)
        }
        val viewLedgerPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt() * 10 + 1,
            ledgerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: WhatsApp Remind Intent
        val reminderMsg = ReminderUtils.createReminderMessage(contactName, amount, System.currentTimeMillis())
        val cleanNumber = phoneNumber?.replace(Regex("[^0-9+]"), "") ?: ""
        val waUri = if (cleanNumber.isNotEmpty()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(reminderMsg)}")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(reminderMsg)}")
        }
        val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val waPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt() * 10 + 2,
            waIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Snooze 24H Broadcast
        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra("reminderId", reminderId)
            putExtra("contactId", contactId)
            putExtra("contactName", contactName)
            putExtra("amount", amount)
            putExtra("phoneNumber", phoneNumber)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() * 10 + 3,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmt = CurrencyFormatter.formatRupee(amount)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Payment Due: $contactName")
            .setContentText("Collection due of $formattedAmt for $contactName. Settle or remind today.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Collection due of $formattedAmt for $contactName is scheduled for today. You can send a WhatsApp reminder, view the full ledger, or snooze for 24 hours."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(viewLedgerPendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "WhatsApp Remind", waPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "View Ledger", viewLedgerPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 24H", snoozePendingIntent)
            .build()

        notificationManager.notify(reminderId.toInt().coerceAtLeast(1001), notification)
    }
}
