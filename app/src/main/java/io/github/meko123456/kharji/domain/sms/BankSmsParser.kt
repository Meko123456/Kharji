package io.github.meko123456.kharji.domain.sms

import io.github.meko123456.kharji.data.EntrySource

/**
 * Turns a bank SMS into an [SmsTransaction], or null when the message isn't a
 * debit transaction (balance info, OTP codes, marketing, incoming transfers).
 *
 * Pure Kotlin: no Android types, so every format is covered by unit tests. Formats
 * vary between banks and change over time, so parsing is keyword-based and
 * deliberately conservative — anything it isn't sure about returns null rather than
 * inventing an expense, and captures land as `pending` for the user to confirm.
 */
sealed class BankSmsParser(val source: EntrySource) {

    abstract val senderIds: Set<String>
    protected abstract val debitKeywords: List<String>
    protected abstract val merchantMarkers: List<String>

    /**
     * Words that mean "not a debit we should record" even if an amount is present.
     * Note "balance" is NOT here: purchase SMS usually append the remaining balance,
     * so it's only disqualifying when the message is *just* a balance notice — see
     * [isBalanceOnlyNotice].
     */
    protected open val ignoreKeywords: List<String> = listOf(
        "otp", "code", "კოდი", "received", "ჩაირიცხა",
        "refund", "დაბრუნება", "salary", "ხელფასი",
    )

    fun matchesSender(sender: String): Boolean =
        senderIds.any { sender.equals(it, ignoreCase = true) || sender.contains(it, ignoreCase = true) }

    fun parse(body: String): SmsTransaction? {
        val lower = body.lowercase()
        if (ignoreKeywords.any { lower.contains(it) }) return null
        if (isBalanceOnlyNotice(lower)) return null
        if (debitKeywords.none { lower.contains(it) }) return null

        val (minor, currency) = AmountParser.parse(body) ?: return null
        if (minor <= 0) return null

        return SmsTransaction(
            amountMinor = minor,
            currency = currency,
            merchant = extractMerchant(body),
            source = source,
            isDebit = true,
        )
    }

    /**
     * True when the message only reports a balance (no debit wording), e.g.
     * "Your balance is 1,240.00 GEL" — as opposed to a purchase that happens to
     * mention the balance afterwards.
     */
    private fun isBalanceOnlyNotice(lower: String): Boolean {
        val mentionsBalance = lower.contains("balance") || lower.contains("ბალანსი")
        return mentionsBalance && debitKeywords.none { lower.contains(it) }
    }

    /** Text after a marker like "at"/"Merchant:" up to the next delimiter. */
    private fun extractMerchant(body: String): String? {
        for (marker in merchantMarkers) {
            val idx = body.indexOf(marker, ignoreCase = true)
            if (idx < 0) continue
            val rest = body.substring(idx + marker.length).trimStart(' ', ':', '-', '،', ',')
            val merchant = rest.takeWhile { it != ',' && it != ';' && it != '\n' && it != '.' }.trim()
            if (merchant.isNotEmpty() && merchant.length <= 40) return merchant
        }
        return null
    }

    /** TBC Bank. */
    data object Tbc : BankSmsParser(EntrySource.SMS_TBC) {
        override val senderIds = setOf("TBC", "TBCBank", "TBC Bank")
        override val debitKeywords = listOf(
            "purchase", "payment", "paid", "withdrawal", "transaction",
            "გადახდა", "ჩამოიჭრა", "განაღდება",
        )
        override val merchantMarkers = listOf("at ", "Merchant:", "merchant ", "ობიექტი:")
    }

    /** Bank of Georgia. */
    data object Bog : BankSmsParser(EntrySource.SMS_BOG) {
        override val senderIds = setOf("BOG", "BankofGeorgia", "Bank of Georgia", "SakartveloBank")
        override val debitKeywords = listOf(
            "purchase", "payment", "paid", "withdrawal", "spent", "transaction",
            "გადახდა", "ჩამოიჭრა", "განაღდება",
        )
        override val merchantMarkers = listOf("at ", "Merchant:", "merchant ", "ობიექტი:")
    }

    companion object {
        // Lazy: the companion initializes before the nested `data object`s below it,
        // so building this eagerly would capture nulls.
        val all: List<BankSmsParser> by lazy { listOf(Tbc, Bog) }

        /** Picks the parser for a sender id and parses, or null if unrecognised. */
        fun parse(sender: String, body: String): SmsTransaction? =
            all.firstOrNull { it.matchesSender(sender) }?.parse(body)
    }
}
