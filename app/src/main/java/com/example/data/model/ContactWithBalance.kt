package com.example.data.model

data class ContactWithBalance(
    val contact: Contact,
    val netBalance: Double, // Positive = You Got (Credit, user will get), Negative = You Gave (Debit, user will pay)
    val lastActivityTime: Long
)
