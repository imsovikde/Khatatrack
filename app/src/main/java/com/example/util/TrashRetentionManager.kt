package com.example.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.TrashCleanupWorker
import java.util.concurrent.TimeUnit

object TrashRetentionManager {
    private const val PREFS_NAME = "khata_trash_prefs"
    private const val KEY_RETENTION_DAYS = "retention_days"
    const val WORK_NAME = "TrashCleanupWork"

    val AVAILABLE_RETENTION_OPTIONS = listOf(7, 15, 30, 60, 90)

    fun getRetentionDays(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_RETENTION_DAYS, 30)
    }

    fun setRetentionDays(context: Context, days: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_RETENTION_DAYS, days).apply()
        scheduleDailyCleanupWork(context)
    }

    fun scheduleDailyCleanupWork(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<TrashCleanupWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
