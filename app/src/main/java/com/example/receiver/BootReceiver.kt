package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.KhataDatabase
import com.example.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = KhataDatabase.getDatabase(context)
                    val pendingReminders = db.reminderDao().getPendingReminders().first()

                    for (reminder in pendingReminders) {
                        val contact = if (reminder.contactId != null) db.contactDao().getContactByIdSync(reminder.contactId) else null
                        if (contact != null && reminder.reminderDate > System.currentTimeMillis()) {
                            val netBalance = db.contactDao().getContactNetBalanceSync(contact.id) ?: 0.0
                            ReminderScheduler.scheduleExactReminder(
                                context = context,
                                reminderId = reminder.id,
                                contactId = contact.id,
                                contactName = contact.name,
                                amount = Math.abs(netBalance),
                                phoneNumber = contact.mobileNumber,
                                triggerAtMillis = reminder.reminderDate
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
