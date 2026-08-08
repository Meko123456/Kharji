package io.github.meko123456.kharji.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** Currencies Kharji handles. All use 2 fraction digits. */
enum class KCurrency(val code: String, val symbol: String) {
    GEL("GEL", "₾"),
    AED("AED", "د.إ"),
    USD("USD", "$"),
    EUR("EUR", "€"),
}

/**
 * An exact amount in a currency's minor units (tetri, fils, cents).
 * All arithmetic is integer-based — no floating point drift.
 */
data class Money(val minor: Long, val currency: KCurrency) {

    operator fun plus(other: Money): Money {
        require(other.currency == currency) {
            "Cannot add ${other.currency.code} to ${currency.code} without conversion"
        }
        return copy(minor = Math.addExact(minor, other.minor))
    }

    operator fun minus(other: Money): Money {
        require(other.currency == currency) {
            "Cannot subtract ${other.currency.code} from ${currency.code} without conversion"
        }
        return copy(minor = Math.subtractExact(minor, other.minor))
    }

    val isNegative: Boolean get() = minor < 0

    /** "₾12.50", "-د.إ3.07", "$0.00" */
    fun format(): String {
        val sign = if (minor < 0) "-" else ""
        val abs = if (minor == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(minor)
        return "$sign${currency.symbol}${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    companion object {
        /** Parse a decimal amount ("12.5", "12.50", "12") into minor units. */
        fun of(amount: String, currency: KCurrency): Money {
            val minor = BigDecimal(amount.trim())
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact()
            return Money(minor, currency)
        }
    }
}

/** A cached FX rate: 1 unit of [from] buys [rate] units of [to], as of [asOfEpochDay]. */
data class FxQuote(
    val from: KCurrency,
    val to: KCurrency,
    val rate: Double,
    val asOfEpochDay: Long,
) {
    init {
        require(rate > 0) { "rate must be positive" }
    }

    /** Older than [maxAgeDays] relative to [todayEpochDay]. */
    fun isStale(todayEpochDay: Long, maxAgeDays: Long = 2): Boolean =
        todayEpochDay - asOfEpochDay > maxAgeDays
}

object CurrencyConverter {

    /**
     * Converts [money] using [quote]; rounds half-even on minor units.
     * The quote must match the money's currency exactly.
     */
    fun convert(money: Money, quote: FxQuote): Money {
        require(quote.from == money.currency) {
            "Quote is ${quote.from.code}->${quote.to.code}, money is ${money.currency.code}"
        }
        val converted = BigDecimal(money.minor)
            .multiply(BigDecimal(quote.rate.toString()))
            .setScale(0, RoundingMode.HALF_EVEN)
            .longValueExact()
        return Money(converted, quote.to)
    }
}
