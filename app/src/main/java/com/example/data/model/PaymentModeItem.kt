package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_modes")
data class PaymentModeItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "Payments",
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false
)
