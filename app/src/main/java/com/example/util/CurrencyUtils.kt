package com.example.util

import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.roundToLong

object CurrencyUtils {
    private val indianCurrencyFormat = DecimalFormat("##,##,##0.00")
    private val indianIntegerFormat = DecimalFormat("##,##,##0")

    /**
     * Converts a rupee amount (Double) to integer paise (Long).
     * e.g. 1250.50 -> 125050L, 450.0 -> 45000L, 0.05 -> 5L.
     * Uses roundToLong() to prevent IEEE 754 precision inaccuracies.
     */
    fun rupeesToPaise(rupees: Double): Long {
        if (rupees.isNaN() || rupees.isInfinite()) return 0L
        return (rupees * 100.0).roundToLong()
    }

    /**
     * Parses a user-entered rupee string to paise (Long).
     * Strips currency symbols, spaces, and commas.
     * e.g. "₹1,250.50" -> 125050L, "350" -> 35000L.
     */
    fun parseRupeesToPaise(rupeesText: String): Long {
        val sanitized = rupeesText.replace("₹", "").replace(",", "").trim()
        val parsedDouble = sanitized.toDoubleOrNull() ?: return 0L
        return rupeesToPaise(parsedDouble)
    }

    /**
     * Converts paise (Long) to rupees (Double) for external API communication.
     * e.g. 125050L -> 1250.50
     */
    fun paiseToRupees(paise: Long): Double {
        return paise / 100.0
    }

    /**
     * Formats paise as currency string with ₹ symbol and two decimal places.
     * e.g. 125050L -> "₹1,250.50", 45000L -> "₹450.00"
     */
    fun formatPaise(paise: Long, showSymbol: Boolean = true): String {
        val rupees = paise / 100.0
        val formatted = synchronized(indianCurrencyFormat) {
            indianCurrencyFormat.format(rupees)
        }
        return if (showSymbol) "₹$formatted" else formatted
    }

    /**
     * Formats paise without decimals when paise % 100 == 0.
     * Useful for text input pre-fills.
     * e.g. 35000L -> "350", 125050L -> "1250.50"
     */
    fun formatPaiseForInput(paise: Long): String {
        return if (paise % 100L == 0L) {
            (paise / 100L).toString()
        } else {
            String.format(Locale.US, "%.2f", paise / 100.0)
        }
    }

    /**
     * Calculates revenue in paise from physical quantity and unit rate in paise.
     * e.g. 10.5 kg at 2550 paise (₹25.50) -> 26775 paise (₹267.75)
     */
    fun calculateRevenuePaise(quantity: Double, ratePerUnitPaise: Long): Long {
        if (quantity <= 0.0 || ratePerUnitPaise <= 0L) return 0L
        return (quantity * ratePerUnitPaise).roundToLong()
    }

    /**
     * Calculates earned wage in paise for an attendance record:
     * - "PRESENT" -> 1.0 day wage (dailyWageRatePaise)
     * - "HALF_DAY" -> 0.5 day wage (dailyWageRatePaise / 2L)
     * - "ABSENT" / other -> 0L
     */
    fun calculateDailyWagePaise(status: String, dailyWageRatePaise: Long): Long {
        return when (status) {
            "PRESENT" -> dailyWageRatePaise
            "HALF_DAY" -> dailyWageRatePaise / 2L
            else -> 0L
        }
    }

    /**
     * Calculates total earned wage in paise for a worker:
     * (presentCount * dailyWageRatePaise) + (halfDayCount * (dailyWageRatePaise / 2L))
     */
    fun calculateTotalEarnedPaise(presentCount: Int, halfDayCount: Int, dailyWageRatePaise: Long): Long {
        return (presentCount.toLong() * dailyWageRatePaise) + (halfDayCount.toLong() * (dailyWageRatePaise / 2L))
    }

    /**
     * Calculates remaining worker balance in paise:
     * totalEarnedPaise - totalAdvancePaise - totalSalaryPaise
     */
    fun calculateWorkerBalancePaise(
        presentCount: Int,
        halfDayCount: Int,
        dailyWageRatePaise: Long,
        totalAdvancePaise: Long,
        totalSalaryPaise: Long
    ): Long {
        val earned = calculateTotalEarnedPaise(presentCount, halfDayCount, dailyWageRatePaise)
        return earned - totalAdvancePaise - totalSalaryPaise
    }
}
