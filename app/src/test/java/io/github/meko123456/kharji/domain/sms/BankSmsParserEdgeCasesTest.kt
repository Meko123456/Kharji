package io.github.meko123456.kharji.domain.sms

import io.github.meko123456.kharji.domain.KCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Edge cases that real bank traffic throws at the parser. Formats drift over time, so
 * these pin down the behaviour we rely on: never invent an expense, never double-count,
 * and prefer the *transaction* amount over any balance mentioned alongside it.
 */
class BankSmsParserEdgeCasesTest {

    @Test
    fun picksTransactionAmountNotTrailingBalance() {
        val tx = BankSmsParser.Tbc.parse("Purchase 12.50 GEL at SPAR, balance 340.00 GEL")!!
        assertEquals("must use the purchase amount, not the balance", 1250L, tx.amountMinor)
    }

    @Test
    fun handlesLowercaseKeywords() {
        val tx = BankSmsParser.Bog.parse("payment 5.00 GEL at kiosk")!!
        assertEquals(500L, tx.amountMinor)
    }

    @Test
    fun handlesSenderWithSuffix() {
        val tx = BankSmsParser.parse("TBCBank", "Purchase 3.20 GEL at Cafe")!!
        assertEquals(320L, tx.amountMinor)
    }

    @Test
    fun senderMatchIsCaseInsensitive() {
        assertTrue(BankSmsParser.Tbc.matchesSender("tbc"))
        assertTrue(BankSmsParser.Bog.matchesSender("bank of georgia"))
    }

    @Test
    fun zeroAmountIsRejected() {
        assertNull(BankSmsParser.Tbc.parse("Purchase 0.00 GEL at Test"))
    }

    @Test
    fun merchantWithSpacesIsKept() {
        val tx = BankSmsParser.Bog.parse("Purchase 18.00 GEL at Goodwill Saburtalo, balance 12.00 GEL")!!
        assertEquals("Goodwill Saburtalo", tx.merchant)
    }

    @Test
    fun absurdlyLongMerchantIsDropped() {
        val long = "X".repeat(80)
        val tx = BankSmsParser.Bog.parse("Purchase 18.00 GEL at $long")!!
        assertNull("over-long merchant text is more likely a parse artefact", tx.merchant)
    }

    @Test
    fun foreignCurrencyKeepsItsOwnCurrency() {
        val tx = BankSmsParser.Tbc.parse("Payment USD 19.99 at Steam")!!
        assertEquals(KCurrency.USD, tx.currency)
        assertEquals(1999L, tx.amountMinor)
    }

    @Test
    fun everyCaptureIsMarkedDebit() {
        val tx = BankSmsParser.Tbc.parse("Withdrawal 100.00 GEL")!!
        assertTrue(tx.isDebit)
    }

    @Test
    fun emptyOrJunkBodyIsIgnored() {
        assertNull(BankSmsParser.Tbc.parse(""))
        assertNull(BankSmsParser.Tbc.parse("......"))
    }

    @Test
    fun declinedTransactionsAreNotRecorded() {
        // No amount present, so nothing to record even though "purchase" appears.
        assertNull(BankSmsParser.Tbc.parse("Purchase declined - insufficient funds"))
    }
}
