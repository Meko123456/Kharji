package io.github.meko123456.kharji.domain.receipt

import io.github.meko123456.kharji.domain.KCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptParserTest {

    @Test
    fun picksLabelledTotalNotTheBiggestLineItem() {
        val receipt = listOf(
            "SPAR SABURTALO",
            "Milk 2.40",
            "Bread 1.80",
            "Cheese 15.60",
            "TOTAL 19.80",
        )
        val r = ReceiptParser.parse(receipt)!!
        assertEquals(1980L, r.amountMinor)
    }

    @Test
    fun readsTotalFromTheFollowingLine() {
        val receipt = listOf("Goodwill", "Item 5.00", "TOTAL", "12.30")
        assertEquals(1230L, ReceiptParser.parse(receipt)!!.amountMinor)
    }

    @Test
    fun ignoresVatAndChangeLines() {
        val receipt = listOf(
            "Carrefour",
            "TOTAL 24.00",
            "VAT 18% 4.32",
            "CASH 50.00",
            "CHANGE 26.00",
        )
        assertEquals(2400L, ReceiptParser.parse(receipt)!!.amountMinor)
    }

    @Test
    fun fallsBackToLargestAmountWhenNoTotalLabel() {
        val receipt = listOf("Kiosk", "Water 1.20", "Sandwich 6.50")
        assertEquals(650L, ReceiptParser.parse(receipt)!!.amountMinor)
    }

    @Test
    fun cashLineIsNeverTheFallbackTotal() {
        val receipt = listOf("Shop", "Item 8.00", "CASH 100.00")
        assertEquals(800L, ReceiptParser.parse(receipt)!!.amountMinor)
    }

    @Test
    fun usesExplicitCurrencyWhenPresent() {
        val receipt = listOf("CARREFOUR DUBAI", "TOTAL AED 45.00")
        val r = ReceiptParser.parse(receipt)!!
        assertEquals(KCurrency.AED, r.currency)
        assertEquals(4500L, r.amountMinor)
    }

    @Test
    fun fallsBackToDefaultCurrency() {
        val r = ReceiptParser.parse(listOf("SPAR", "TOTAL 19.80"), KCurrency.GEL)!!
        assertEquals(KCurrency.GEL, r.currency)
    }

    @Test
    fun readsGeorgianTotalLabel() {
        val receipt = listOf("ნიკორა", "პური 1.50", "სულ 8.40")
        assertEquals(840L, ReceiptParser.parse(receipt)!!.amountMinor)
    }

    @Test
    fun guessesMerchantFromTheHeader() {
        val receipt = listOf("SPAR SABURTALO", "Fiscal receipt", "TOTAL 19.80")
        assertEquals("SPAR SABURTALO", ReceiptParser.parse(receipt)!!.merchant)
    }

    @Test
    fun skipsReceiptBoilerplateWhenGuessingMerchant() {
        val receipt = listOf("FISCAL RECEIPT", "Goodwill", "TOTAL 19.80")
        assertEquals("Goodwill", ReceiptParser.parse(receipt)!!.merchant)
    }

    @Test
    fun emptyOrTextOnlyReceiptYieldsNull() {
        assertNull(ReceiptParser.parse(emptyList()))
        assertNull(ReceiptParser.parse(listOf("thank you", "come again")))
    }

    @Test
    fun handlesCommaDecimalReceipts() {
        assertEquals(1980L, ReceiptParser.parse(listOf("Shop", "TOTAL 19,80"))!!.amountMinor)
    }
}
