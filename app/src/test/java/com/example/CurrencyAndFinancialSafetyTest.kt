package com.example

import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyAndFinancialSafetyTest {

    @Test
    fun testRupeesToPaiseConversion() {
        assertEquals(35000L, CurrencyUtils.rupeesToPaise(350.0))
        assertEquals(35050L, CurrencyUtils.rupeesToPaise(350.50))
        assertEquals(35075L, CurrencyUtils.rupeesToPaise(350.75))
        assertEquals(100L, CurrencyUtils.rupeesToPaise(1.0))
        assertEquals(0L, CurrencyUtils.rupeesToPaise(0.0))
    }

    @Test
    fun testPaiseToRupeesConversion() {
        assertEquals(350.0, CurrencyUtils.paiseToRupees(35000L), 0.001)
        assertEquals(350.50, CurrencyUtils.paiseToRupees(35050L), 0.001)
        assertEquals(0.0, CurrencyUtils.paiseToRupees(0L), 0.001)
    }

    @Test
    fun testParseRupeesToPaise() {
        assertEquals(35000L, CurrencyUtils.parseRupeesToPaise("350"))
        assertEquals(35050L, CurrencyUtils.parseRupeesToPaise("350.50"))
        assertEquals(125025L, CurrencyUtils.parseRupeesToPaise("1,250.25"))
        assertEquals(0L, CurrencyUtils.parseRupeesToPaise(""))
        assertEquals(0L, CurrencyUtils.parseRupeesToPaise("abc"))
    }

    @Test
    fun testFormatPaiseToRupeesString() {
        assertEquals("350", CurrencyUtils.formatPaiseForInput(35000L))
        assertEquals("350.50", CurrencyUtils.formatPaiseForInput(35050L))
        assertEquals("350", DateUtils.formatPaiseToRupeesString(35000L))
        assertEquals("350.75", DateUtils.formatPaiseToRupeesString(35075L))
    }

    @Test
    fun testCalculateRevenuePaise() {
        // 5.5 quintals at ₹2,400.50 per quintal (240050 paise)
        val ratePaise = 240050L
        val quantity = 5.5
        val revenuePaise = CurrencyUtils.calculateRevenuePaise(quantity, ratePaise)
        // 5.5 * 240050 = 1,320,275 paise (₹13,202.75)
        assertEquals(1320275L, revenuePaise)
    }

    @Test
    fun testCalculateWorkerEarnedWage() {
        // Worker with ₹450 daily wage (45000 paise)
        val dailyWagePaise = 45000L
        val presentDays = 12
        val halfDays = 3

        // 12 * 45000 + 3 * 22500 = 540,000 + 67,500 = 607,500 paise (₹6,075.00)
        val earned = CurrencyUtils.calculateTotalEarnedPaise(presentDays, halfDays, dailyWagePaise)
        assertEquals(607500L, earned)
    }

    @Test
    fun testWorkerNetBalanceCalculation() {
        // Worker earned ₹6,075.00 (607500 paise)
        val dailyWagePaise = 45000L
        val presentDays = 12
        val halfDays = 3
        val totalAdvances = 150000L // ₹1,500 advance
        val totalSalaryPaid = 350000L // ₹3,500 salary paid

        // Net balance due: 607500 - (150000 + 350000) = 107500 paise (₹1,075.00)
        val balanceDue = CurrencyUtils.calculateWorkerBalancePaise(
            presentCount = presentDays,
            halfDayCount = halfDays,
            dailyWageRatePaise = dailyWagePaise,
            totalAdvancePaise = totalAdvances,
            totalSalaryPaise = totalSalaryPaid
        )
        assertEquals(107500L, balanceDue)
    }

    @Test
    fun testNoFloatingPointDriftInLargeTransactionVolumes() {
        // Simulate 10,000 transactions of ₹33.33 (3333 paise)
        var totalPaise = 0L
        for (i in 1..10000) {
            totalPaise += 3333L
        }
        assertEquals(33330000L, totalPaise)
        assertEquals(333300.0, CurrencyUtils.paiseToRupees(totalPaise), 0.001)
        assertTrue(DateUtils.formatCurrency(totalPaise).contains("333,300.00"))
    }

    @Test
    fun testExpenseTotalsSummation() {
        val expenses = listOf(
            150000L, // Diesel: ₹1,500
            285075L, // Fertilizer: ₹2,850.75
            45000L,  // Seeds: ₹450
            999900L  // Tractor: ₹9,999
        )
        val total = expenses.sum()
        assertEquals(1479975L, total)
        assertEquals(14799.75, CurrencyUtils.paiseToRupees(total), 0.001)
    }
}
