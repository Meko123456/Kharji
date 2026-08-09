package io.github.meko123456.kharji.data.fx

import io.github.meko123456.kharji.domain.KCurrency
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ErApiResponse(
    val result: String,
    @SerialName("base_code") val baseCode: String = "",
    val rates: Map<String, Double> = emptyMap(),
)

/** Maps a raw response to the rates we track (silently drops unknown codes). */
fun ErApiResponse.toKnownRates(): Map<KCurrency, Double> =
    rates.mapNotNull { (code, rate) ->
        KCurrency.entries.firstOrNull { it.code == code }?.let { it to rate }
    }.toMap()

/**
 * FX rates from open.er-api.com — free, no API key, daily-updated,
 * covers GEL and AED (unlike ECB-based sources).
 */
class FxClient(private val http: HttpClient = defaultHttpClient()) {

    suspend fun latest(base: KCurrency): Result<Map<KCurrency, Double>> = runCatching {
        val response: ErApiResponse = http.get("https://open.er-api.com/v6/latest/${base.code}").body()
        check(response.result == "success") { "FX API returned ${response.result}" }
        response.toKnownRates()
    }

    companion object {
        fun defaultHttpClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
