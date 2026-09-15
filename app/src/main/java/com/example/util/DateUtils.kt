package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun today(): String {
        return isoFormat.format(Date())
    }

    fun formatForDisplay(isoDate: String): String {
        if (isoDate.isBlank()) return ""
        return try {
            val date = isoFormat.parse(isoDate)
            if (date != null) displayFormat.format(date) else isoDate
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatWorkerCount(count: Int): String {
        return if (count == 1) "1 worker" else "$count workers"
    }

    fun formatRecordCount(count: Int, label: String = "record"): String {
        return if (count == 1) "1 $label" else "$count ${label}s"
    }

    fun formatDaysRemaining(days: Long): String {
        return when {
            days < 0 -> "Overdue by ${kotlin.math.abs(days)} ${if (kotlin.math.abs(days) == 1L) "day" else "days"}"
            days == 0L -> "Harvest Today"
            days == 1L -> "Harvest in 1 day"
            else -> "Harvest in $days days"
        }
    }

    fun daysBetween(fromIsoDate: String, toIsoDate: String): Long {
        return try {
            val from = isoFormat.parse(fromIsoDate)
            val to = isoFormat.parse(toIsoDate)
            if (from != null && to != null) {
                val diff = to.time - from.time
                TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
            } else 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun daysFromToday(targetIsoDate: String): Long {
        return daysBetween(today(), targetIsoDate)
    }

    fun offsetDate(isoDate: String, days: Int): String {
        return try {
            val cal = Calendar.getInstance()
            val parsed = isoFormat.parse(isoDate)
            if (parsed != null) {
                cal.time = parsed
            }
            cal.add(Calendar.DAY_OF_YEAR, days)
            isoFormat.format(cal.time)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatCurrency(amount: Double): String {
        return String.format(Locale.getDefault(), "₹%.2f", amount)
    }

    fun formatCurrency(paise: Long): String {
        return CurrencyUtils.formatPaise(paise)
    }

    fun formatNumber(number: Double): String {
        return if (number % 1.0 == 0.0) {
            number.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", number)
        }
    }

    fun formatPaiseToRupeesString(paise: Long): String {
        return CurrencyUtils.formatPaiseForInput(paise)
    }
}
