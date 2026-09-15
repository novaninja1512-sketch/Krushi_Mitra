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

    @Test
    fun testOddWageAndHalfDayCalculations() {
        // Daily wage ₹375 (37500 paise). Half day should be exactly 18750 paise.
        val wage375 = 37500L
        val earned1 = CurrencyUtils.calculateTotalEarnedPaise(
            presentCount = 3,
            halfDayCount = 1,
            dailyWageRatePaise = wage375
        )
        // 3 * 37500 + 18750 = 112500 + 18750 = 131250 paise (₹1,312.50)
        assertEquals(131250L, earned1)

        // Odd daily wage ₹333.33 (33333 paise). Half day is 33333 / 2 = 16666 paise.
        val wageOdd = 33333L
        val earnedOdd = CurrencyUtils.calculateTotalEarnedPaise(
            presentCount = 2,
            halfDayCount = 1,
            dailyWageRatePaise = wageOdd
        )
        // 2 * 33333 + 16666 = 66666 + 16666 = 83332 paise
        assertEquals(83332L, earnedOdd)
    }

    @Test
    fun testMultiAdvanceBalances() {
        val dailyWage = 40000L // ₹400/day
        val presentDays = 15
        val halfDays = 2
        // Total earned: 15 * 40000 + 2 * 20000 = 600,000 + 40,000 = 640,000 paise (₹6,400.00)

        // 4 separate advances: ₹500, ₹250, ₹1000, ₹350
        val advances = listOf(50000L, 25000L, 100000L, 35000L)
        val totalAdvances = advances.sum() // 210,000 paise (₹2,100.00)

        // 2 salary payouts: ₹2000, ₹1500
        val salaries = listOf(200000L, 150000L)
        val totalSalaries = salaries.sum() // 350,000 paise (₹3,500.00)

        val balance = CurrencyUtils.calculateWorkerBalancePaise(
            presentCount = presentDays,
            halfDayCount = halfDays,
            dailyWageRatePaise = dailyWage,
            totalAdvancePaise = totalAdvances,
            totalSalaryPaise = totalSalaries
        )
        // Balance: 640,000 - 210,000 - 350,000 = 80,000 paise (₹800.00)
        assertEquals(80000L, balance)
    }

    @Test
    fun testEdgeAndExtremeMonetaryValues() {
        // Minimum non-zero value: 1 paisa (₹0.01)
        assertEquals(1L, CurrencyUtils.rupeesToPaise(0.01))
        assertEquals(0.01, CurrencyUtils.paiseToRupees(1L), 0.0001)

        // Large monetary value: ₹5,000,000.00 (50 lakh rupees = 500,000,000 paise)
        val largeRupees = 5000000.0
        val largePaise = CurrencyUtils.rupeesToPaise(largeRupees)
        assertEquals(500000000L, largePaise)
        assertEquals(5000000.0, CurrencyUtils.paiseToRupees(largePaise), 0.001)

        // Exact fractional crop yield calculation: 12.375 quintals at ₹3,250.60 per quintal (325060 paise)
        // 12.375 * 325060 = 4,022,617.5 -> rounded to 4,022,618 paise (₹40,226.18)
        val cropRevenue = CurrencyUtils.calculateRevenuePaise(12.375, 325060L)
        assertEquals(4022618L, cropRevenue)
    }

    @Test
    fun testFinancialAdversarialMatrix_ExactPaiseZeroDrift() {
        // 1. Decimal quantity x rate tests from Section 15:
        // 0.1 x ₹10 (1000 paise) = 100 paise (₹1.00)
        assertEquals(100L, CurrencyUtils.calculateRevenuePaise(0.1, 1000L))

        // 0.25 x ₹32 (3200 paise) = 800 paise (₹8.00)
        assertEquals(800L, CurrencyUtils.calculateRevenuePaise(0.25, 3200L))

        // 1.5 x ₹125.50 (12550 paise) = 18825 paise (₹188.25)
        assertEquals(18825L, CurrencyUtils.calculateRevenuePaise(1.5, 12550L))

        // 10.75 x ₹37.25 (3725 paise) = 40043.75 -> 40044 paise (₹400.44)
        assertEquals(40044L, CurrencyUtils.calculateRevenuePaise(10.75, 3725L))

        // 2. Realistic agricultural quantity:
        // 154.35 quintals at ₹2,150.25 per quintal (215025 paise)
        // 154.35 * 215025 = 33,189,108.75 -> 33,189,109 paise (₹3,31,891.09)
        val agRevenue = CurrencyUtils.calculateRevenuePaise(154.35, 215025L)
        assertEquals(33189109L, agRevenue)

        // 3. Odd daily wage calculation: ₹451 / 2
        // ₹451 = 45100 paise. Half-day = 22550 paise (₹225.50).
        val wage451 = 45100L
        assertEquals(22550L, CurrencyUtils.calculateDailyWagePaise("HALF_DAY", wage451))
        assertEquals(45100L, CurrencyUtils.calculateDailyWagePaise("PRESENT", wage451))

        // 4. Worker balance exactness:
        // 5 full days, 3 half days at ₹451:
        // (5 * 45100) + (3 * 22550) = 225500 + 67650 = 293150 paise (₹2,931.50)
        // Advances: ₹500 (50000 paise), Salary payout: ₹1,500 (150000 paise)
        // Balance: 293150 - 50000 - 150000 = 93150 paise (₹931.50)
        val balance = CurrencyUtils.calculateWorkerBalancePaise(
            presentCount = 5,
            halfDayCount = 3,
            dailyWageRatePaise = wage451,
            totalAdvancePaise = 50000L,
            totalSalaryPaise = 150000L
        )
        assertEquals(93150L, balance)
    }
}
