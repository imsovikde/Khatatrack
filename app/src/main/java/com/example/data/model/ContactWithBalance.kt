package com.example.data.model

data class ContactWithBalance(
    val contact: Contact,
    val netBalance: Double, // Positive = You Gave (Credit, user will get), Negative = You Got (Debit, user will pay)
    val lastActivityTime: Long
)
