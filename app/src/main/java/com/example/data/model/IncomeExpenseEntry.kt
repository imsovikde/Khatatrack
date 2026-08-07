package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_expense_entries")
data class IncomeExpenseEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // INCOME or EXPENSE
    val amount: Double,
    val currency: String = "INR",
    val transactionDate: Long = System.currentTimeMillis(),
    val transactionTime: String = "",
    val paymentMode: String = "Cash",
    val transactionRefId: String? = null,
    val categoryTag: String = "General",
    val note: String? = null,
    val attachmentPhoto: String? = null,
    val collectionDueDate: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_INCOME = "INCOME"
        const val TYPE_EXPENSE = "EXPENSE"
    }
}
