package io.github.meko123456.kharji.ui

import io.github.meko123456.kharji.data.FxRate
import io.github.meko123456.kharji.domain.KCurrency
import io.github.meko123456.kharji.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvertedTotalTest {

    private val today = 20_000L

    private fun gel(minor: Long) = Money(minor, KCurrency.GEL)
    private fun aed(minor: Long) = Money(minor, KCurrency.AED)

    @Test
    fun `gel-only total needs no rates`() {
        val result = convertedTotal(mapOf("GEL" to gel(5000)), emptyList(), today)
        assertEquals(gel(5000), result!!.first)
        assertFalse(result.second)
    }

    @Test
    fun `mixed currencies convert via cached rates`() {
        val totals = mapOf("GEL" to gel(5000), "AED" to aed(10000))
        val rates = listOf(FxRate("AED", "GEL", 0.735, today))
        val result = convertedTotal(totals, rates, today)
        // 50.00 GEL + 100.00 AED * 0.735 = 50.00 + 73.50 = 123.50 GEL
        assertEquals(gel(12350), result!!.first)
        assertFalse(result.second)
    }

    @Test
    fun `missing rate yields null instead of a wrong number`() {
        val totals = mapOf("GEL" to gel(5000), "AED" to aed(10000))
        assertNull(convertedTotal(totals, emptyList(), today))
    }

    @Test
    fun `old rate still converts but flags staleness`() {
        val totals = mapOf("AED" to aed(10000))
        val rates = listOf(FxRate("AED", "GEL", 0.735, asOfEpochDay = today - 5))
        val result = convertedTotal(totals, rates, today)
        assertEquals(gel(7350), result!!.first)
        assertTrue(result.second)
    }
}
