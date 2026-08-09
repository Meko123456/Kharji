package io.github.meko123456.kharji.domain

import io.github.meko123456.kharji.data.Category
import io.github.meko123456.kharji.data.Entry
import io.github.meko123456.kharji.data.EntrySource
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExporterTest {

    private val food = Category(id = 1, name = "Food", emoji = "🍔")

    private fun entry(
        minor: Long,
        currency: String = "GEL",
        merchant: String? = null,
        note: String? = null,
        epochDay: Long = 20_675, // 2026-08-10, fixed
    ) = Entry(
        id = 1,
        amountMinor = minor,
        currency = currency,
        categoryId = 1,
        merchant = merchant,
        note = note,
        epochDay = epochDay,
        createdAtMillis = 0,
        source = EntrySource.MANUAL,
    )

    @Test
    fun `plain fields pass through unquoted`() {
        assertEquals("hello", CsvExporter.escape("hello"))
        assertEquals("", CsvExporter.escape(""))
    }

    @Test
    fun `commas quotes and newlines get quoted and doubled`() {
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""))
        assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"))
    }

    @Test
    fun `exports header plus one row per entry`() {
        val csv = CsvExporter.toCsv(listOf(entry(1250, merchant = "Spar, Vake")), listOf(food))
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        assertEquals("date,amount,currency,category,merchant,note,source", lines[0])
        assertEquals("2026-08-10,12.50,GEL,Food,\"Spar, Vake\",,MANUAL", lines[1])
    }

    @Test
    fun `amount formats minor units with two decimals`() {
        val csv = CsvExporter.toCsv(listOf(entry(5)), listOf(food))
        assert(csv.contains(",0.05,GEL,"))
    }
}
