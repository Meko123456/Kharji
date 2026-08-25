package io.github.meko123456.kharji.domain.sms

import io.github.meko123456.kharji.data.EntrySource
import io.github.meko123456.kharji.domain.KCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSmsParserTest {

    // ---- sender routing ------------------------------------------------------

    @Test
    fun routesTbcSender() {
        val tx = BankSmsParser.parse("TBC", "Purchase 12.50 GEL at SPAR")!!
        assertEquals(EntrySource.SMS_TBC, tx.source)
    }

    @Test
    fun routesBogSender() {
        val tx = BankSmsParser.parse("BankofGeorgia", "Purchase 12.50 GEL at SPAR")!!
        assertEquals(EntrySource.SMS_BOG, tx.source)
    }

    @Test
    fun unknownSenderIsIgnored() {
        assertNull(BankSmsParser.parse("VODAFONE", "Purchase 12.50 GEL at SPAR"))
    }

    // ---- debit detection -----------------------------------------------------

    @Test
    fun parsesPurchaseAmountAndCurrency() {
        val tx = BankSmsParser.Tbc.parse("Purchase 12.50 GEL at SPAR, balance 340.00 GEL")!!
        assertEquals(1250L, tx.amountMinor)
        assertEquals(KCurrency.GEL, tx.currency)
        assertTrue(tx.isDebit)
    }

    @Test
    fun parsesWithdrawal() {
        val tx = BankSmsParser.Bog.parse("Withdrawal 200 GEL ATM Rustaveli")!!
        assertEquals(20000L, tx.amountMinor)
    }

    @Test
    fun parsesGeorgianLanguageDebit() {
        val tx = BankSmsParser.Tbc.parse("გადახდა 15,40 GEL, ობიექტი: Carrefour")!!
        assertEquals(1540L, tx.amountMinor)
        assertEquals("Carrefour", tx.merchant)
    }

    @Test
    fun parsesAedPurchaseAbroad() {
        val tx = BankSmsParser.Tbc.parse("Payment AED 45.00 at CARREFOUR DUBAI")!!
        assertEquals(4500L, tx.amountMinor)
        assertEquals(KCurrency.AED, tx.currency)
    }

    // ---- merchant extraction -------------------------------------------------

    @Test
    fun extractsMerchantAfterAt() {
        val tx = BankSmsParser.Tbc.parse("Purchase 12.50 GEL at SPAR, balance 340.00 GEL")!!
        assertEquals("SPAR", tx.merchant)
    }

    @Test
    fun extractsMerchantAfterLabel() {
        val tx = BankSmsParser.Bog.parse("Payment 9.90 GEL Merchant: Wolt Georgia")!!
        assertEquals("Wolt Georgia", tx.merchant)
    }

    @Test
    fun missingMerchantIsNull() {
        val tx = BankSmsParser.Bog.parse("Withdrawal 200.00 GEL")!!
        assertNull(tx.merchant)
    }

    // ---- things we must NOT record ------------------------------------------

    @Test
    fun ignoresOtpMessages() {
        assertNull(BankSmsParser.Tbc.parse("Your OTP code 1234. Do not share. Payment 50.00 GEL"))
    }

    @Test
    fun ignoresIncomingTransfer() {
        assertNull(BankSmsParser.Tbc.parse("Received 500.00 GEL from John"))
    }

    @Test
    fun ignoresBalanceNotice() {
        assertNull(BankSmsParser.Bog.parse("Your balance is 1,240.00 GEL"))
    }

    @Test
    fun ignoresRefund() {
        assertNull(BankSmsParser.Bog.parse("Refund 20.00 GEL processed"))
    }

    @Test
    fun ignoresMarketingWithoutDebitKeyword() {
        assertNull(BankSmsParser.Tbc.parse("Get a loan of 5000 GEL today!"))
    }

    @Test
    fun ignoresDebitWithoutAmount() {
        assertNull(BankSmsParser.Tbc.parse("Purchase declined at SPAR"))
    }
}
