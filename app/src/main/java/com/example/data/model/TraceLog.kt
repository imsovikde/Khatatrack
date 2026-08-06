package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trace_logs")
data class TraceLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String, // "CONTACT" or "TRANSACTION"
    val entityId: Long,
    val entityName: String = "", // Descriptive title (e.g. Contact name, Transaction amount/type)
    val action: String, // "CREATE", "EDIT", "DELETE", "RESTORE", "PURGE"
    val fieldChanged: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
