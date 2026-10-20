package io.github.meko123456.kharji.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseFromTextTest {

    @Test
    fun `an amount with its currency fills in both`() {
        assertEquals(ExpenseFromText("23.50", KCurrency.GEL, ""), ExpenseFromText.from("Paid 23.50 GEL at Carrefour"))
        assertEquals(ExpenseFromText("1234.56", KCurrency.GEL, ""), ExpenseFromText.from("₾1 234,56"))
        assertEquals(ExpenseFromText("1299.00", KCurrency.USD, ""), ExpenseFromText.from("USD 1,299.00"))
    }

    @Test
    fun `text that is only a number fills in the amount`() {
        assertEquals(ExpenseFromText("23.50", null, ""), ExpenseFromText.from(" 23,50\n"))
        assertEquals(ExpenseFromText("1234.5", null, ""), ExpenseFromText.from("1 234,5"))
    }

    @Test
    fun `other text is the merchant, numbers in it included`() {
        assertEquals(ExpenseFromText("", null, "Carrefour 24/7"), ExpenseFromText.from("  Carrefour 24/7\nSaburtalo branch"))
        assertEquals(ExpenseFromText("", null, ""), ExpenseFromText.from(" \n "))
    }

    @Test
    fun `a long merchant is cut short, never inside an emoji`() {
        val name = "a".repeat(79) + "🍕 pizza"
        assertEquals("a".repeat(79), ExpenseFromText.from(name).merchant)
        assertEquals(80, ExpenseFromText.from("b".repeat(100)).merchant.length)
    }
}
