package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.KhataDatabase
import com.example.data.repository.KhataRepository
import com.example.util.TrashRetentionManager

class CleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = KhataDatabase.getDatabase(applicationContext)
            val repository = KhataRepository(
                contactDao = db.contactDao(),
                transactionDao = db.transactionDao(),
                reminderDao = db.reminderDao(),
                traceLogDao = db.traceLogDao(),
                categoryDao = db.categoryDao(),
                paymentModeDao = db.paymentModeDao(),
                incomeExpenseEntryDao = db.incomeExpenseEntryDao()
            )

            val retentionDays = TrashRetentionManager.getRetentionDays(applicationContext)
            repository.purgeOldTrash(retentionDays)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

typealias TrashCleanupWorker = CleanupWorker
