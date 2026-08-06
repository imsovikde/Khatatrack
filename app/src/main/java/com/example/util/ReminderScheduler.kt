package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.receiver.ReminderReceiver

object ReminderScheduler {

    fun scheduleExactReminder(
        context: Context,
        reminderId: Long,
        contactId: Long,
        contactName: String,
        amount: Double,
        phoneNumber: String?,
        triggerAtMillis: Long
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Check exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Fallback: Use setAndAllowWhileIdle or normal alarm
                val intent = createReminderIntent(context, reminderId, contactId, contactName, amount, phoneNumber)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminderId.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return
            }
        }

        val intent = createReminderIntent(context, reminderId, contactId, contactName, amount, phoneNumber)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancelExactReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun createReminderIntent(
        context: Context,
        reminderId: Long,
        contactId: Long,
        contactName: String,
        amount: Double,
        phoneNumber: String?
    ): Intent {
        return Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminderId", reminderId)
            putExtra("contactId", contactId)
            putExtra("contactName", contactName)
            putExtra("amount", amount)
            putExtra("phoneNumber", phoneNumber)
        }
    }
}
