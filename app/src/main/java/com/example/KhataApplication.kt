package com.example

import android.app.Application
import com.example.util.TrashRetentionManager

class KhataApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Initialize WorkManager daily periodic cleanup worker for expired trash items
            TrashRetentionManager.scheduleDailyCleanupWork(this)
            // Initialize WorkManager daily overdue digest worker
            com.example.worker.OverdueDigestWorker.scheduleDailyDigestWork(this)
        } catch (e: Exception) {
            // Gracefully handle unit test environments where WorkManager is not initialized
            e.printStackTrace()
        }
    }
}
