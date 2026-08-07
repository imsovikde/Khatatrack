package com.example.util

import android.content.Context

object CurrencyFormatter {
    private var activeCurrencySymbol: String = "₹"
    private var activeCurrencyCode: String = "INR"

    fun updateActiveCurrency(symbol: String, code: String) {
        activeCurrencySymbol = symbol
        activeCurrencyCode = code
    }

    /**
     * Formats monetary value using current currency settings
     */
    fun formatRupee(
        amount: Double,
        showSymbol: Boolean = true,
        includeDecimalsIfAny: Boolean = false,
        symbol: String = activeCurrencySymbol
    ): String {
        val absAmount = Math.abs(amount)
        val formattedNumber = try {
            val currencyInfo = CurrencyManager.CURRENCIES.find { it.symbol == symbol || it.code == activeCurrencyCode }
                ?: CurrencyManager.CURRENCIES[0]
            CurrencyManager.formatAmount(absAmount, currencyInfo, showSymbol = false, includeDecimalsIfAny = includeDecimalsIfAny)
        } catch (e: Exception) {
            String.format("%,.0f", absAmount)
        }

        val prefix = if (showSymbol) "$symbol " else ""
        return "$prefix$formattedNumber"
    }

    /**
     * Formats signed amount string, e.g. "+ ₹2,500" or "- ₹1,850"
     */
    fun formatSignedRupee(amount: Double, symbol: String = activeCurrencySymbol): String {
        val signSymbol = if (amount > 0) "+ $symbol " else if (amount < 0) "- $symbol " else "$symbol "
        val formattedNumber = formatRupee(Math.abs(amount), showSymbol = false, symbol = symbol)
        return "$signSymbol$formattedNumber"
    }

    fun getActiveCurrencySymbol(): String = activeCurrencySymbol
    fun getActiveCurrencyCode(): String = activeCurrencyCode
}
