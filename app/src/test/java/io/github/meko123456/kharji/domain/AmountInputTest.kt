package io.github.meko123456.kharji.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountInputTest {

    @Test
    fun `a decimal comma is read as a point`() {
        assertEquals("1.5", AmountInput.normalize("1,5"))
        assertEquals("12.50", AmountInput.normalize("12,50"))
    }

    @Test
    fun `plain amounts pass through`() {
        assertEquals("12.50", AmountInput.normalize("12.50"))
        assertEquals("12", AmountInput.normalize(" 12 "))
    }

    @Test
    fun `thousands separators and spaces are dropped`() {
        assertEquals("1234.50", AmountInput.normalize("1,234.50"))
        assertEquals("1234.50", AmountInput.normalize("1.234,50"))
        assertEquals("1234567", AmountInput.normalize("1,234,567"))
        assertEquals("1000", AmountInput.normalize("1 000"))
        assertEquals("1000", AmountInput.normalize("1 000"))
    }

    @Test
    fun `anything else is refused`() {
        assertNull(AmountInput.normalize(""))
        assertNull(AmountInput.normalize("abc"))
        assertNull(AmountInput.normalize("1e3"))
        assertNull(AmountInput.normalize("1.2.3"))
        assertNull(AmountInput.normalize("1,"))
    }
}
