package com.example.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    /**
     * Formats monetary value with Indian Digit Grouping (e.g. ₹1,25,000 or ₹2,500)
     */
    fun formatRupee(amount: Double, showSymbol: Boolean = true, includeDecimalsIfAny: Boolean = false): String {
        val absAmount = Math.abs(amount)
        val formattedNumber = try {
            val indiaLocale = Locale("en", "IN")
            val formatter = NumberFormat.getNumberInstance(indiaLocale)
            if (includeDecimalsIfAny && (absAmount % 1.0 != 0.0)) {
                formatter.minimumFractionDigits = 2
                formatter.maximumFractionDigits = 2
            } else {
                formatter.maximumFractionDigits = 0
            }
            formatter.format(absAmount)
        } catch (e: Exception) {
            val decimalFormat = DecimalFormat("#,##,##0")
            decimalFormat.format(absAmount)
        }

        val prefix = if (showSymbol) "₹" else ""
        return "$prefix$formattedNumber"
    }

    /**
     * Formats signed amount string, e.g. "+ ₹2,500" or "- ₹1,850"
     */
    fun formatSignedRupee(amount: Double): String {
        val symbol = if (amount > 0) "+ ₹" else if (amount < 0) "- ₹" else "₹"
        val formattedNumber = formatRupee(Math.abs(amount), showSymbol = false)
        return "$symbol$formattedNumber"
    }
}
