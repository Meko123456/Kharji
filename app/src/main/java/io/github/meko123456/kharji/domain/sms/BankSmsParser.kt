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

    /**
     * Sender ids this bank uses: SMS short names, and the titles its messages arrive under.
     *
     * Matched as whole words, never as substrings — see [matchesSender] for why that distinction
     * decides whether a contact called Bogdan can create an expense.
     */
    abstract val senderIds: Set<String>

    /**
     * Exact package names of this bank's own apps, for notifications the bank posts itself rather
     * than as an SMS.
     *
     * Exact, deliberately. A package name is attacker-chosen: matching it loosely means any app
     * that happens to contain "bog" somewhere in its id gets treated as Bank of Georgia. The cost
     * of being strict is that a bank app that renames or ships under an id not listed here stops
     * being recognised by package — it still matches on its notification title, and a missed
     * capture is the right way for this to fail.
     */
    open val packageNames: Set<String> = emptySet()

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

    /**
     * Whether [sender] identifies this bank.
     *
     * Word-boundary matching, not substring. Substring matching is what the first version did, and
     * it is wrong in a way that is easy to miss: the id "BOG" is inside "Bogdan", "Bogota" and
     * "bogus", so a message from a friend reading "paid 50 GEL for the tickets" was enough to file
     * a Bank of Georgia expense. The user would find a transaction they never made, from a bank
     * that never sent anything.
     *
     * So both sides are cut into alphanumeric words and the id has to appear as a whole run of
     * them. "BOG*CARREFOUR" and "TBC Bank: purchase" still match, because the delimiters are not
     * letters; "Bogdan" no longer does, because "bogdan" is one word and it is not "bog".
     * Multi-word ids like "Bank of Georgia" must appear in order and adjacent, so "Georgia Bank of
     * Commerce" does not match either.
     */
    fun matchesSender(sender: String): Boolean {
        val words = words(sender)
        return senderIds.any { id -> containsRun(words, words(id)) }
    }

    /** Whether [packageName] is one of this bank's own apps. Exact, case-insensitive. */
    fun matchesPackage(packageName: String): Boolean =
        packageNames.any { it.equals(packageName, ignoreCase = true) }

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

    /**
     * Lowercased alphanumeric words. Unicode-aware, so Georgian sender ids split the same way Latin
     * ones do; everything else is a boundary, which is what makes "BOG*CARREFOUR" two words.
     */
    private fun words(value: String): List<String> {
        val out = mutableListOf<String>()
        val word = StringBuilder()
        for (ch in value) {
            if (ch.isLetterOrDigit()) {
                word.append(ch.lowercaseChar())
            } else if (word.isNotEmpty()) {
                out += word.toString()
                word.clear()
            }
        }
        if (word.isNotEmpty()) out += word.toString()
        return out
    }

    /** Whether [needle] appears in [haystack] as consecutive whole words. */
    private fun containsRun(haystack: List<String>, needle: List<String>): Boolean {
        if (needle.isEmpty() || needle.size > haystack.size) return false
        return (0..haystack.size - needle.size).any { start ->
            needle.indices.all { haystack[start + it] == needle[it] }
        }
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
        override val packageNames = setOf("ge.tbcbank.mobile")
        override val debitKeywords = listOf(
            "purchase", "payment", "paid", "withdrawal", "transaction",
            "გადახდა", "ჩამოიჭრა", "განაღდება",
        )
        override val merchantMarkers = listOf("at ", "Merchant:", "merchant ", "ობიექტი:")
    }

    /** Bank of Georgia. */
    data object Bog : BankSmsParser(EntrySource.SMS_BOG) {
        override val senderIds = setOf("BOG", "BankofGeorgia", "Bank of Georgia", "SakartveloBank")
        override val packageNames = setOf("ge.bog.mobilebank")
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

        /** The bank whose sender id [sender] carries, or null if none does. */
        fun forSender(sender: String): BankSmsParser? = all.firstOrNull { it.matchesSender(sender) }

        /** The bank that owns the app with this exact package name, or null. */
        fun forPackage(packageName: String): BankSmsParser? =
            all.firstOrNull { it.matchesPackage(packageName) }

        /** Picks the parser for a sender id and parses, or null if unrecognised. */
        fun parse(sender: String, body: String): SmsTransaction? = forSender(sender)?.parse(body)
    }
}
