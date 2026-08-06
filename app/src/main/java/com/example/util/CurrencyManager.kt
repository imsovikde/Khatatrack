package com.example.util

import android.content.Context
import android.content.SharedPreferences
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String,
    val locale: Locale = Locale.US
)

object CurrencyManager {
    val CURRENCIES = listOf(
        CurrencyInfo("INR", "₹", "Indian Rupee", Locale("en", "IN")),
        CurrencyInfo("USD", "$", "US Dollar", Locale.US),
        CurrencyInfo("EUR", "€", "Euro", Locale.GERMANY),
        CurrencyInfo("GBP", "£", "British Pound", Locale.UK),
        CurrencyInfo("AED", "AED", "UAE Dirham", Locale("ar", "AE")),
        CurrencyInfo("CAD", "CA$", "Canadian Dollar", Locale.CANADA),
        CurrencyInfo("AUD", "A$", "Australian Dollar", Locale("en", "AU")),
        CurrencyInfo("JPY", "¥", "Japanese Yen", Locale.JAPAN),
        CurrencyInfo("SGD", "S$", "Singapore Dollar", Locale("en", "SG")),
        CurrencyInfo("SAR", "SAR", "Saudi Riyal", Locale("ar", "SA")),
        CurrencyInfo("MYR", "RM", "Malaysian Ringgit", Locale("ms", "MY")),
        CurrencyInfo("BDT", "৳", "Bangladeshi Taka", Locale("bn", "BD")),
        CurrencyInfo("PKR", "₨", "Pakistani Rupee", Locale("ur", "PK")),
        CurrencyInfo("NPR", "NPR", "Nepalese Rupee", Locale("ne", "NP")),
        CurrencyInfo("LKR", "Rs", "Sri Lankan Rupee", Locale("si", "LK")),
        CurrencyInfo("PHP", "₱", "Philippine Peso", Locale("en", "PH")),
        CurrencyInfo("IDR", "Rp", "Indonesian Rupiah", Locale("id", "ID")),
        CurrencyInfo("BRL", "R$", "Brazilian Real", Locale("pt", "BR")),
        CurrencyInfo("ZAR", "R", "South African Rand", Locale("en", "ZA")),
        CurrencyInfo("CHF", "CHF", "Swiss Franc", Locale("de", "CH")),
        CurrencyInfo("CNY", "¥", "Chinese Yuan", Locale.CHINA)
    )

    private const val PREFS_NAME = "khatatrack_settings"
    private const val KEY_CURRENCY_CODE = "selected_currency_code"
    private const val KEY_RETENTION_DAYS = "trash_retention_days"
    private const val KEY_ENCRYPTION_ENABLED = "backup_encryption_enabled"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSelectedCurrency(context: Context): CurrencyInfo {
        val prefs = getPrefs(context)
        val code = prefs.getString(KEY_CURRENCY_CODE, "INR") ?: "INR"
        return CURRENCIES.find { it.code == code } ?: CURRENCIES[0]
    }

    fun setSelectedCurrency(context: Context, code: String) {
        getPrefs(context).edit().putString(KEY_CURRENCY_CODE, code).apply()
    }

    fun getRetentionDays(context: Context): Int {
        return getPrefs(context).getInt(KEY_RETENTION_DAYS, 30)
    }

    fun setRetentionDays(context: Context, days: Int) {
        getPrefs(context).edit().putInt(KEY_RETENTION_DAYS, days).apply()
    }

    fun isEncryptionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENCRYPTION_ENABLED, true)
    }

    fun setEncryptionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENCRYPTION_ENABLED, enabled).apply()
    }

    fun formatAmount(
        amount: Double,
        currency: CurrencyInfo,
        showSymbol: Boolean = true,
        includeDecimalsIfAny: Boolean = false
    ): String {
        val absAmount = Math.abs(amount)
        val formattedNumber = try {
            val formatter = NumberFormat.getNumberInstance(currency.locale)
            if (includeDecimalsIfAny && (absAmount % 1.0 != 0.0)) {
                formatter.minimumFractionDigits = 2
                formatter.maximumFractionDigits = 2
            } else {
                formatter.maximumFractionDigits = 0
            }
            formatter.format(absAmount)
        } catch (e: Exception) {
            val decimalFormat = DecimalFormat("#,##0")
            decimalFormat.format(absAmount)
        }

        val prefix = if (showSymbol) "${currency.symbol} " else ""
        return "$prefix$formattedNumber"
    }
}
