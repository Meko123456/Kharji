package io.github.meko123456.kharji.domain.sms

import io.github.meko123456.kharji.domain.KCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountParserTest {

    // ---- toMinor -------------------------------------------------------------

    @Test
    fun dotDecimal() = assertEquals(1250L, AmountParser.toMinor("12.50"))

    @Test
    fun commaDecimal() = assertEquals(1250L, AmountParser.toMinor("12,50"))

    @Test
    fun singleFractionDigitIsTenths() = assertEquals(1250L, AmountParser.toMinor("12.5"))

    @Test
    fun wholeNumberHasNoCents() = assertEquals(70000L, AmountParser.toMinor("700"))

    @Test
    fun thousandsCommaWithDotDecimal() = assertEquals(123456L, AmountParser.toMinor("1,234.56"))

    @Test
    fun thousandsSpaceWithCommaDecimal() = assertEquals(123456L, AmountParser.toMinor("1 234,56"))

    @Test
    fun thousandsDotWithCommaDecimal() = assertEquals(123456L, AmountParser.toMinor("1.234,56"))

    @Test
    fun groupedThousandsWithoutDecimals() = assertEquals(123400L, AmountParser.toMinor("1,234"))

    @Test
    fun rejectsNonNumeric() {
        assertNull(AmountParser.toMinor(""))
        assertNull(AmountParser.toMinor("abc"))
    }

    // ---- parse (amount + currency) ------------------------------------------

    @Test
    fun amountThenCode() {
        val (minor, cur) = AmountParser.parse("Purchase 12.50 GEL at SPAR")!!
        assertEquals(1250L, minor)
        assertEquals(KCurrency.GEL, cur)
    }

    @Test
    fun codeThenAmount() {
        val (minor, cur) = AmountParser.parse("Payment AED 45,00 processed")!!
        assertEquals(4500L, minor)
        assertEquals(KCurrency.AED, cur)
    }

    @Test
    fun symbolThenAmount() {
        val (minor, cur) = AmountParser.parse("Paid ₾7.30 at Wissol")!!
        assertEquals(730L, minor)
        assertEquals(KCurrency.GEL, cur)
    }

    @Test
    fun amountThenSymbol() {
        val (minor, cur) = AmountParser.parse("Charged 25.00$ today")!!
        assertEquals(2500L, minor)
        assertEquals(KCurrency.USD, cur)
    }

    @Test
    fun largeGroupedAmount() {
        val (minor, cur) = AmountParser.parse("Transaction 1,234.56 USD")!!
        assertEquals(123456L, minor)
        assertEquals(KCurrency.USD, cur)
    }

    @Test
    fun ungroupedThousandsAfterTheCurrencyAreReadWhole() {
        // The grouped form matched its first three digits and stopped, so these read as
        // USD 129.00, ₾150.00 and AED 123.00.
        assertEquals(129900L to KCurrency.USD, AmountParser.parse("Payment USD 1299.00 at Apple"))
        assertEquals(150000L to KCurrency.GEL, AmountParser.parse("Paid ₾1500 at IKEA"))
        assertEquals(1234500L to KCurrency.AED, AmountParser.parse("Payment AED 12345 at Emaar"))
    }

    @Test
    fun groupedAndSmallAmountsAfterTheCurrencyStillParse() {
        assertEquals(129900L to KCurrency.USD, AmountParser.parse("Payment USD 1,299.00 at Apple"))
        assertEquals(123456L to KCurrency.GEL, AmountParser.parse("GEL 1 234,56"))
        assertEquals(1999L to KCurrency.USD, AmountParser.parse("Payment USD 19.99 at Steam"))
        assertEquals(129900L to KCurrency.USD, AmountParser.parse("Charged 1299.00$ today"))
    }

    @Test
    fun unknownCurrencyCodeIsIgnored() {
        assertNull(AmountParser.parse("Payment 10.00 XYZ"))
    }

    @Test
    fun textWithoutAmountIsNull() {
        assertNull(AmountParser.parse("Your OTP code is ready"))
    }
}
