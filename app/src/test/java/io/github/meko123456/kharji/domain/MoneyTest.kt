package io.github.meko123456.kharji.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    private val gel = { m: Long -> Money(m, KCurrency.GEL) }
    private val aed = { m: Long -> Money(m, KCurrency.AED) }

    // --- arithmetic ---

    @Test
    fun `same-currency addition and subtraction`() {
        assertEquals(gel(1750), gel(1000) + gel(750))
        assertEquals(gel(250), gel(1000) - gel(750))
        assertEquals(gel(-500), gel(250) - gel(750))
    }

    @Test
    fun `cross-currency arithmetic throws`() {
        assertThrows(IllegalArgumentException::class.java) { gel(100) + aed(100) }
        assertThrows(IllegalArgumentException::class.java) { gel(100) - aed(100) }
    }

    @Test
    fun `overflow is loud, not silent`() {
        assertThrows(ArithmeticException::class.java) { gel(Long.MAX_VALUE) + gel(1) }
    }

    // --- parsing ---

    @Test
    fun `parses decimal strings into minor units`() {
        assertEquals(gel(1250), Money.of("12.50", KCurrency.GEL))
        assertEquals(gel(1250), Money.of("12.5", KCurrency.GEL))
        assertEquals(gel(1200), Money.of("12", KCurrency.GEL))
        assertEquals(gel(-307), Money.of("-3.07", KCurrency.GEL))
    }

    @Test
    fun `parsing rounds sub-minor digits half-even`() {
        assertEquals(gel(1250), Money.of("12.505", KCurrency.GEL))  // .5 -> nearest even
        assertEquals(gel(1252), Money.of("12.515", KCurrency.GEL))
        assertEquals(gel(1250), Money.of("12.5049", KCurrency.GEL))
    }

    // --- formatting ---

    @Test
    fun `formats with symbol and two decimals`() {
        assertEquals("₾12.50", gel(1250).format())
        assertEquals("₾0.05", gel(5).format())
        assertEquals("$0.00", Money(0, KCurrency.USD).format())
        assertEquals("-₾3.07", gel(-307).format())
    }

    // --- conversion ---

    @Test
    fun `converts with half-even rounding`() {
        val quote = FxQuote(KCurrency.GEL, KCurrency.AED, 1.36, asOfEpochDay = 20_000)
        // 10.00 GEL * 1.36 = 13.60 AED
        assertEquals(Money(1360, KCurrency.AED), CurrencyConverter.convert(gel(1000), quote))
    }

    @Test
    fun `conversion requires matching source currency`() {
        val quote = FxQuote(KCurrency.USD, KCurrency.AED, 3.67, asOfEpochDay = 20_000)
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyConverter.convert(gel(1000), quote)
        }
    }

    @Test
    fun `zero and negative rates rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            FxQuote(KCurrency.GEL, KCurrency.AED, 0.0, 20_000)
        }
    }

    // --- staleness ---

    @Test
    fun `quote staleness respects max age`() {
        val quote = FxQuote(KCurrency.GEL, KCurrency.AED, 1.36, asOfEpochDay = 100)
        assertFalse(quote.isStale(todayEpochDay = 102))
        assertTrue(quote.isStale(todayEpochDay = 103))
    }
}
