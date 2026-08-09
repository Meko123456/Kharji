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

    /** Quote when the field contains comma, quote, or newline; double inner quotes. */
    internal fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
}
