package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.KhataDatabase
import com.example.data.model.TraceLog
import com.example.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminderId", 0L)
        val contactId = intent.getLongExtra("contactId", 0L)
        val contactName = intent.getStringExtra("contactName") ?: "Customer"
        val amount = intent.getDoubleExtra("amount", 0.0)
        val phoneNumber = intent.getStringExtra("phoneNumber")

        // 1. Dismiss notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt().coerceAtLeast(1001))

        // 2. Schedule for +24 hours
        val snoozeTimeMillis = System.currentTimeMillis() + 86400000L // 24 hours

        ReminderScheduler.scheduleExactReminder(
            context = context,
            reminderId = reminderId,
            contactId = contactId,
            contactName = contactName,
            amount = amount,
            phoneNumber = phoneNumber,
            triggerAtMillis = snoozeTimeMillis
        )

        // 3. Update database and log EDIT in trace log asynchronously
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = KhataDatabase.getDatabase(context)

                // Log trace
                db.traceLogDao().insertTrace(
                    TraceLog(
                        action = "EDIT",
                        entityType = "REMINDER",
                        entityId = reminderId,
                        entityName = contactName,
                        fieldChanged = "Reminder snoozed for 24 hours (₹$amount)"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
