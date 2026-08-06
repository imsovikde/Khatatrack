package com.example

import android.app.Application
import com.example.util.TrashRetentionManager

class KhataApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize WorkManager daily periodic cleanup worker for expired trash items
        TrashRetentionManager.scheduleDailyCleanupWork(this)
    }
}
