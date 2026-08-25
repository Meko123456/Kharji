package io.github.meko123456.kharji.domain.sms

import io.github.meko123456.kharji.domain.KCurrency

/**
 * Extracts an amount + currency from bank-SMS text.
 *
 * Georgian bank SMS put the currency on either side of the number and use both
 * decimal separators, so this handles `12.50 GEL`, `GEL 12,50`, `₾12.50`, and
 * thousands separators (`1,234.56` / `1 234,56`). Pure and exhaustively testable.
 */
object AmountParser {

    private val symbols = mapOf(
        "₾" to KCurrency.GEL,
        "$" to KCurrency.USD,
        "€" to KCurrency.EUR,
        "د.إ" to KCurrency.AED,
    )

    private val codes = KCurrency.entries.associateBy { it.code }

    /** A number like 12.50, 1,234.56, 1 234,56 — captured as group 1. */
    private const val NUMBER = "(\\d{1,3}(?:[ ,.]\\d{3})*(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)"

    // "12.50 GEL" / "12.50GEL"
    private val amountThenCode = Regex("$NUMBER\\s*([A-Z]{3})")
    // "GEL 12.50"
    private val codeThenAmount = Regex("([A-Z]{3})\\s*$NUMBER")
    // "₾12.50"
    private val symbolThenAmount = Regex("([₾$€]|د\\.إ)\\s*$NUMBER")
    // "12.50₾"
    private val amountThenSymbol = Regex("$NUMBER\\s*([₾$€]|د\\.إ)")

    /** Returns the first amount+currency found, or null if the text has none. */
    fun parse(text: String): Pair<Long, KCurrency>? {
        amountThenCode.find(text)?.let { m ->
            codes[m.groupValues[2]]?.let { c -> toMinor(m.groupValues[1])?.let { return it to c } }
        }
        codeThenAmount.find(text)?.let { m ->
            codes[m.groupValues[1]]?.let { c -> toMinor(m.groupValues[2])?.let { return it to c } }
        }
        symbolThenAmount.find(text)?.let { m ->
            symbols[m.groupValues[1]]?.let { c -> toMinor(m.groupValues[2])?.let { return it to c } }
        }
        amountThenSymbol.find(text)?.let { m ->
            symbols[m.groupValues[2]]?.let { c -> toMinor(m.groupValues[1])?.let { return it to c } }
        }
        return null
    }

    /**
     * "1,234.56" / "1 234,56" / "12.5" / "700" → minor units.
     * The last '.' or ',' followed by 1–2 digits is the decimal separator; any other
     * separator is a thousands grouping and is stripped.
     */
    fun toMinor(raw: String): Long? {
        val cleaned = raw.trim().replace(" ", "")
        if (cleaned.isEmpty()) return null

        val lastDot = cleaned.lastIndexOf('.')
        val lastComma = cleaned.lastIndexOf(',')
        val sepIndex = maxOf(lastDot, lastComma)

        val (intPart, fracPart) = if (sepIndex >= 0 && cleaned.length - sepIndex - 1 in 1..2) {
            cleaned.substring(0, sepIndex) to cleaned.substring(sepIndex + 1)
        } else {
            cleaned to ""
        }

        val digitsOnly = intPart.filter { it.isDigit() }
        if (digitsOnly.isEmpty() || !fracPart.all { it.isDigit() }) return null

        val units = digitsOnly.toLongOrNull() ?: return null
        val cents = when (fracPart.length) {
            0 -> 0L
            1 -> fracPart.toLong() * 10
            else -> fracPart.toLong()
        }
        return units * 100 + cents
    }
}
