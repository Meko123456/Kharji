package io.github.meko123456.kharji.domain

/**
 * An amount as someone types it, made into the plain decimal [Money.of] reads.
 *
 * The amount field's decimal keypad has a comma key as well as a point, and in Georgia the comma
 * is the decimal mark, so "1,5" for one lari fifty is what many people type. It was refused as
 * "Enter a valid amount". So: spaces go; with both marks, the last one is the decimal point and
 * the other separates thousands; a single comma on its own is a decimal point, and several are
 * thousands separators. Anything that is not then digits with at most one decimal point, such as
 * "1e3", is refused.
 */
object AmountInput {

    private val plain = Regex("""-?\d+(\.\d+)?""")

    fun normalize(text: String): String? {
        val compact = text.filterNot { it.isWhitespace() || it == ' ' || it == ' ' }
        val lastComma = compact.lastIndexOf(',')
        val lastPoint = compact.lastIndexOf('.')
        val normalized = when {
            lastComma >= 0 && lastPoint >= 0 ->
                if (lastComma > lastPoint) compact.replace(".", "").replace(',', '.') else compact.replace(",", "")
            compact.count { it == ',' } == 1 -> compact.replace(',', '.')
            lastComma >= 0 -> compact.replace(",", "")
            else -> compact
        }
        return normalized.takeIf { plain.matches(it) }
    }
}
