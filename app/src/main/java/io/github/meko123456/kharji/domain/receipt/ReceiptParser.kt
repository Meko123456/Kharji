package io.github.meko123456.kharji.domain.receipt

import io.github.meko123456.kharji.domain.KCurrency
import io.github.meko123456.kharji.domain.sms.AmountParser

/** What we managed to read off a receipt. Everything is a guess the user confirms. */
data class ScannedReceipt(
    val amountMinor: Long,
    val currency: KCurrency,
    val merchant: String?,
)

/**
 * Turns OCR'd receipt lines into a total + merchant guess.
 *
 * Pure Kotlin (no ML Kit or Android types) so the tricky part — picking the *total*
 * out of a wall of prices — is exhaustively unit-tested. Strategy:
 *  1. Prefer a line labelled TOTAL / სულ / ჯამი / AMOUNT DUE, taking its amount.
 *  2. Otherwise fall back to the largest plausible amount on the receipt.
 * Lines about VAT, change, cash tendered or card numbers are never used as the total.
 */
object ReceiptParser {

    private val totalLabels = listOf(
        "total", "amount due", "grand total", "to pay", "balance due",
        "სულ", "ჯამი", "გადასახდელი",
    )

    /** Labels that carry a number we must NOT mistake for the total. */
    private val excludedLabels = listOf(
        "vat", "tax", "დღგ", "change", "ხურდა", "cash", "tendered",
        "card", "subtotal", "discount", "ფასდაკლება", "tip",
    )

    fun parse(lines: List<String>, defaultCurrency: KCurrency = KCurrency.GEL): ScannedReceipt? {
        val clean = lines.map { it.trim() }.filter { it.isNotBlank() }
        if (clean.isEmpty()) return null

        val total = totalFromLabel(clean) ?: largestPlausibleAmount(clean) ?: return null
        return ScannedReceipt(
            amountMinor = total.first,
            currency = total.second ?: defaultCurrency,
            merchant = merchantGuess(clean),
        )
    }

    /** Amount on a line labelled as the total (or the line right after the label). */
    private fun totalFromLabel(lines: List<String>): Pair<Long, KCurrency?>? {
        lines.forEachIndexed { index, line ->
            val lower = line.lowercase()
            if (excludedLabels.any { lower.contains(it) }) return@forEachIndexed
            if (totalLabels.none { lower.contains(it) }) return@forEachIndexed

            amountOn(line)?.let { return it }
            // "TOTAL" alone on its own line, value on the next one.
            lines.getOrNull(index + 1)?.let { next -> amountOn(next)?.let { return it } }
        }
        return null
    }

    /** Fallback: the biggest number that isn't on an excluded line. */
    private fun largestPlausibleAmount(lines: List<String>): Pair<Long, KCurrency?>? =
        lines.filterNot { line -> excludedLabels.any { line.lowercase().contains(it) } }
            .mapNotNull { amountOn(it) }
            .maxByOrNull { it.first }

    /** First amount on a line: prefers an explicit currency, else a bare number. */
    private fun amountOn(line: String): Pair<Long, KCurrency?>? {
        AmountParser.parse(line)?.let { (minor, currency) -> return minor to currency }
        val bare = Regex("(\\d+(?:[.,]\\d{1,2}))").find(line)?.groupValues?.get(1) ?: return null
        return AmountParser.toMinor(bare)?.let { it to null }
    }

    /**
     * Merchant guess: the first line that reads like a name — letters, not a price,
     * not a receipt header like "FISCAL RECEIPT".
     */
    private fun merchantGuess(lines: List<String>): String? {
        val noise = listOf("receipt", "fiscal", "invoice", "ჩეკი", "receipt no", "tel", "tax id")
        return lines.take(5).firstOrNull { line ->
            val lower = line.lowercase()
            line.any { it.isLetter() } &&
                line.count { it.isDigit() } <= 2 &&
                line.length in 3..40 &&
                noise.none { lower.contains(it) }
        }
    }
}
