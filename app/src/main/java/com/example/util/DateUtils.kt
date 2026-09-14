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
        return try {
            val date = isoFormat.parse(isoDate)
            if (date != null) displayFormat.format(date) else isoDate
        } catch (_: Exception) {
            isoDate
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

    fun formatNumber(number: Double): String {
        return if (number % 1.0 == 0.0) {
            number.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", number)
        }
    }
}
