package io.github.meko123456.kharji.domain

import io.github.meko123456.kharji.domain.sms.AmountParser
import java.util.Locale

/**
 * What text picked out or shared in another app fills a new expense in with.
 *
 * An amount with its currency ("Paid 23.50 GEL", "₾1 234,56") fills in both; text that is only a
 * number ("23,50") fills in the amount. Anything else ("Carrefour") goes in as the merchant: a
 * number inside other words is no amount, or "Carrefour 24/7" would be 24 tetri.
 */
data class ExpenseFromText(val amount: String, val currency: KCurrency?, val merchant: String) {
    companion object {
        fun from(text: String): ExpenseFromText {
            AmountParser.parse(text)?.let { (minor, currency) -> return ExpenseFromText(decimal(minor), currency, "") }
            AmountInput.normalize(text.trim())?.let { return ExpenseFromText(it, null, "") }
            val line = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
            return ExpenseFromText("", null, capped(line))
        }

        private fun decimal(minor: Long) = "%d.%02d".format(Locale.ROOT, minor / 100, minor % 100)

        /** At most [MERCHANT_MAX] chars, never ending on half an emoji. */
        private fun capped(line: String): String {
            val cut = line.take(MERCHANT_MAX)
            return if (cut.lastOrNull()?.isHighSurrogate() == true) cut.dropLast(1) else cut
        }

        private const val MERCHANT_MAX = 80
    }
}
