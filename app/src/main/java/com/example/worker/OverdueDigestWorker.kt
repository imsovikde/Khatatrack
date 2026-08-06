package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.db.KhataDatabase
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class OverdueDigestWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = KhataDatabase.getDatabase(appContext)
            val contacts = db.contactDao().getAllContactsSync()

            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - (7 * 86400000L)

            var overdueCount = 0
            var totalOverdueAmount = 0.0

            for (contact in contacts) {
                val netBalance = db.contactDao().getContactNetBalanceSync(contact.id) ?: 0.0
                // Net balance > 0 means "You'll Get" (credit/debt owed to user)
                if (netBalance > 0) {
                    val txs = db.transactionDao().getAllTransactionsSync().filter { it.contactId == contact.id && !it.isDeleted }
                    val lastActivity = txs.maxOfOrNull { it.transactionDate } ?: contact.createdAt
                    if (lastActivity < sevenDaysAgo) {
                        overdueCount++
                        totalOverdueAmount += netBalance
                    }
                }
            }

            if (overdueCount > 0) {
                sendSummaryNotification(overdueCount, totalOverdueAmount)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendSummaryNotification(overdueCount: Int, totalAmount: Double) {
        val channelId = "khata_digest_channel"
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "KhataTrack Overdue Digest",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily digest of overdue accounts pending collection"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmt = CurrencyFormatter.formatRupee(totalAmount)

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("KhataTrack Overdue Digest")
            .setContentText("$overdueCount contacts have pending dues over 7 days ($formattedAmt total).")
            .setStyle(NotificationCompat.BigTextStyle().bigText("You have $overdueCount accounts with uncollected dues inactive for over 7 days totaling $formattedAmt. Tap to open KhataTrack and send reminders."))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(9999, notification)
    }

    companion object {
        private const val WORK_NAME = "OverdueDigestWorker"

        fun scheduleDailyDigestWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<OverdueDigestWorker>(24, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
