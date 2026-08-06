package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contactId")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val type: String, // "YOU_GAVE" (Debit) or "YOU_GOT" (Credit)
    val amount: Double,
    val transactionDate: Long = System.currentTimeMillis(),
    val transactionTime: String = "",
    val paymentMode: String = "Cash", // Cash, UPI, Bank Transfer, Cheque, Card, Other
    val note: String? = null,
    val attachmentPhoto: String? = null,
    val collectionDueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_YOU_GAVE = "YOU_GAVE"
        const val TYPE_YOU_GOT = "YOU_GOT"
    }
}
