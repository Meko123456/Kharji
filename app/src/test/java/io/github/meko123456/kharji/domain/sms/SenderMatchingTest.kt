package io.github.meko123456.kharji.domain.sms

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sender matching is the whole security boundary of the notification capture: it decides which
 * notifications on the phone are allowed to create a transaction. It used to be a substring test,
 * which meant the three letters "bog" anywhere in a title or package id were enough.
 *
 * The negative cases below are the ones that matter — each is a notification a normal phone shows
 * on a normal day, and each one used to file an expense.
 */
class SenderMatchingTest {

    @Test
    fun aContactWhoseNameContainsTheBankIdIsNotTheBank() {
        assertFalse(BankSmsParser.Bog.matchesSender("Bogdan"))
        assertFalse(BankSmsParser.Bog.matchesSender("Bogdan Petrov"))
        assertNull(BankSmsParser.forSender("Bogdan"))
    }

    @Test
    fun aMessageFromAFriendNoLongerBecomesAnExpense() {
        // The exact shape that got through: a real name containing the id, a debit keyword, and an
        // amount. Before word matching this filed a 50 GEL Bank of Georgia transaction.
        assertNull(BankSmsParser.parse("Bogdan", "paid 50 GEL for the tickets"))
    }

    @Test
    fun ordinaryWordsAndPlacesAreNotBanks() {
        listOf("Bogota", "bogus offer inside", "Flights to Bogota from 199 GEL", "Autobiography")
            .forEach { assertNull("$it matched a bank", BankSmsParser.forSender(it)) }
    }

    @Test
    fun anyAppCannotClaimToBeABankByItsPackageId() {
        assertFalse(BankSmsParser.Bog.matchesPackage("com.bogus.app"))
        assertFalse(BankSmsParser.Bog.matchesPackage("io.bogdan.notes"))
        assertFalse(BankSmsParser.Tbc.matchesPackage("com.example.tbcbank.clone"))
        assertNull(BankSmsParser.forPackage("com.bogus.app"))
    }

    @Test
    fun theRealBankAppIsStillRecognised() {
        assertSame(BankSmsParser.Bog, BankSmsParser.forPackage("ge.bog.mobilebank"))
        assertSame(BankSmsParser.Tbc, BankSmsParser.forPackage("ge.tbcbank.mobile"))
        assertTrue(BankSmsParser.Bog.matchesPackage("GE.BOG.MOBILEBANK"))
    }

    @Test
    fun realSenderFormatsStillMatch() {
        assertTrue(BankSmsParser.Tbc.matchesSender("TBC"))
        assertTrue(BankSmsParser.Tbc.matchesSender("tbc"))
        assertTrue(BankSmsParser.Tbc.matchesSender("TBC Bank"))
        assertTrue(BankSmsParser.Tbc.matchesSender("TBCBank"))
        // Delimiters are not letters, so the id is still its own word either side of them.
        assertTrue(BankSmsParser.Bog.matchesSender("BOG*CARREFOUR"))
        assertTrue(BankSmsParser.Bog.matchesSender("BOG: purchase"))
        assertTrue(BankSmsParser.Bog.matchesSender("bank of georgia"))
        assertTrue(BankSmsParser.Bog.matchesSender("SakartveloBank"))
    }

    @Test
    fun multiWordIdsMustBeAdjacentAndInOrder() {
        assertFalse(BankSmsParser.Bog.matchesSender("Georgia Bank of Commerce"))
        assertFalse(BankSmsParser.Bog.matchesSender("Bank of Cyprus, Georgia branch"))
    }

    @Test
    fun blankAndPunctuationOnlySendersMatchNothing() {
        listOf("", "   ", "***", "\n").forEach {
            assertNull("[$it] matched a bank", BankSmsParser.forSender(it))
        }
    }

    @Test
    fun eachBankStillResolvesToItself() {
        assertSame(BankSmsParser.Tbc, BankSmsParser.forSender("TBC Bank"))
        assertSame(BankSmsParser.Bog, BankSmsParser.forSender("BOG"))
    }
}
