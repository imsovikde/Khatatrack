package com.example.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.CleanupWorker
import java.util.concurrent.TimeUnit

object TrashRetentionManager {
    private const val PREFS_NAME = "khata_trash_prefs"
    const val KEY_RETENTION_WINDOW_DAYS = "retention_window_days"
    private const val KEY_LEGACY_RETENTION = "retention_days"
    const val WORK_NAME = "CleanupWorker"

    val AVAILABLE_RETENTION_OPTIONS = listOf(7, 30, 60, 90)

    fun getRetentionDays(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_RETENTION_WINDOW_DAYS)) {
            return prefs.getInt(KEY_RETENTION_WINDOW_DAYS, 30)
        }
        return prefs.getInt(KEY_LEGACY_RETENTION, 30)
    }

    fun setRetentionDays(context: Context, days: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_RETENTION_WINDOW_DAYS, days)
            .putInt(KEY_LEGACY_RETENTION, days)
            .apply()
        scheduleDailyCleanupWork(context)
    }

    fun scheduleDailyCleanupWork(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
