package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val transactionId: Long? = null,
    val reminderDate: Long,
    val status: String = "PENDING", // PENDING, SENT, DISMISSED
    val createdAt: Long = System.currentTimeMillis()
)
