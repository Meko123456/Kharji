package io.github.meko123456.kharji.data.fx

import io.github.meko123456.kharji.domain.KCurrency
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FxClientTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses er-api response and keeps only known currencies`() {
        val sample = """
            {
              "result": "success",
              "base_code": "GEL",
              "time_last_update_unix": 1786608001,
              "rates": { "GEL": 1.0, "AED": 1.36, "USD": 0.37, "EUR": 0.34, "JPY": 55.1, "XYZ": 9.9 }
            }
        """.trimIndent()

        val response = json.decodeFromString<ErApiResponse>(sample)
        val known = response.toKnownRates()

        assertEquals("success", response.result)
        assertEquals("GEL", response.baseCode)
        assertEquals(1.36, known[KCurrency.AED])
        assertEquals(0.37, known[KCurrency.USD])
        assertNull(known.entries.firstOrNull { it.key.code == "JPY" })
        assertEquals(4, known.size) // GEL, AED, USD, EUR — JPY/XYZ dropped
    }

    @Test
    fun `error result parses without rates`() {
        val sample = """{ "result": "error", "error-type": "invalid-key" }"""
        val response = json.decodeFromString<ErApiResponse>(sample)
        assertEquals("error", response.result)
        assertEquals(0, response.toKnownRates().size)
    }
}
