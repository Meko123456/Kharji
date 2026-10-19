package io.github.meko123456.kharji.domain

import io.github.meko123456.kharji.data.Category
import io.github.meko123456.kharji.data.Entry
import java.time.LocalDate

/** RFC-4180-safe CSV writer for entries. */
object CsvExporter {

    private const val HEADER = "date,amount,currency,category,merchant,note,source"

    fun toCsv(entries: List<Entry>, categories: List<Category>): String {
        val categoryById = categories.associateBy { it.id }
        return buildString {
            appendLine(HEADER)
            entries.sortedByDescending { it.epochDay }.forEach { e ->
                val amount = Money(e.amountMinor, KCurrency.valueOf(e.currency))
                    .let { "${it.minor / 100}.${(it.minor % 100).toString().padStart(2, '0')}" }
                appendLine(
                    listOf(
                        LocalDate.ofEpochDay(e.epochDay).toString(),
                        amount,
                        e.currency,
                        e.categoryId?.let { categoryById[it]?.name }.orEmpty(),
                        e.merchant.orEmpty(),
                        e.note.orEmpty(),
                        e.source.name,
                    ).joinToString(",") { escape(it) },
                )
            }
        }
    }

    /**
     * What the exported file holds: the CSV behind a UTF-8 byte-order mark. Excel opens a CSV
     * without one in the system's ANSI code page, which turns every Georgian category, merchant
     * and note into mojibake; with it, Excel reads the file as UTF-8. Google Sheets, Numbers and
     * LibreOffice take the mark as what it is and drop it.
     */
    fun toFile(entries: List<Entry>, categories: List<Category>): String = "\uFEFF" + toCsv(entries, categories)

    /**
     * Quote when the field contains comma, quote, or newline; double inner quotes.
     *
     * A field that opens with =, +, -, @, a tab or a carriage return is a formula to Excel and Sheets,
     * and a merchant comes from a notification whose sender anyone can spoof: one captured "at =1+2"
     * opened as 3. Such a field gets a leading ', which both read as "this is text".
     */
    internal fun escape(field: String): String {
        val text = if (field.firstOrNull() in formulaStarts) "'$field" else field
        return if (text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${text.replace("\"", "\"\"")}\""
        } else {
            text
        }
    }

    private val formulaStarts = setOf('=', '+', '-', '@', '\t', '\r')
}
