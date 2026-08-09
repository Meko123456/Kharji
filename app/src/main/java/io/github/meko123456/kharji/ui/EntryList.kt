package io.github.meko123456.kharji.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.meko123456.kharji.data.Category
import io.github.meko123456.kharji.data.Entry
import io.github.meko123456.kharji.data.FxRate
import io.github.meko123456.kharji.domain.CurrencyConverter
import io.github.meko123456.kharji.domain.FxQuote
import io.github.meko123456.kharji.domain.KCurrency
import io.github.meko123456.kharji.domain.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DayFormat = DateTimeFormatter.ofPattern("EEE, d MMM")

/**
 * Grand total of [totals] in GEL via cached [rates].
 * Returns null when a needed rate is missing; second value marks staleness.
 */
internal fun convertedTotal(
    totals: Map<String, Money>,
    rates: List<FxRate>,
    todayEpochDay: Long,
): Pair<Money, Boolean>? {
    var sumMinor = 0L
    var stale = false
    for ((code, money) in totals) {
        if (code == KCurrency.GEL.code) {
            sumMinor += money.minor
            continue
        }
        val rate = rates.firstOrNull { it.fromCode == code && it.toCode == KCurrency.GEL.code }
            ?: return null
        val quote = FxQuote(KCurrency.valueOf(code), KCurrency.GEL, rate.rate, rate.asOfEpochDay)
        if (quote.isStale(todayEpochDay)) stale = true
        sumMinor += CurrencyConverter.convert(money, quote).minor
    }
    return Money(sumMinor, KCurrency.GEL) to stale
}

/** Current-month totals per currency + day-grouped entry list. Long-press deletes. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryList(
    entries: List<Entry>,
    categories: List<Category>,
    rates: List<FxRate>,
    onDelete: (Entry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryById = categories.associateBy { it.id }
    val thisMonth = LocalDate.now().let { it.year to it.monthValue }
    val monthTotals = entries
        .filter { LocalDate.ofEpochDay(it.epochDay).let { d -> d.year to d.monthValue } == thisMonth }
        .groupBy { it.currency }
        .mapValues { (code, list) -> Money(list.sumOf { it.amountMinor }, KCurrency.valueOf(code)) }

    val byDay = entries.groupBy { it.epochDay }.toSortedMap(compareByDescending { it })

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item(key = "summary") {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("This month", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = monthTotals.values.joinToString("  ·  ") { it.format() }
                            .ifEmpty { "Nothing yet" },
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (monthTotals.size > 1) {
                        val today = LocalDate.now().toEpochDay()
                        convertedTotal(monthTotals, rates, today)?.let { (total, stale) ->
                            Text(
                                text = "≈ ${total.format()} total" + if (stale) "  (rates stale)" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
        byDay.forEach { (epochDay, dayEntries) ->
            item(key = "day-$epochDay") {
                Text(
                    text = LocalDate.ofEpochDay(epochDay).format(DayFormat),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(dayEntries, key = { it.id }) { entry ->
                val category = entry.categoryId?.let(categoryById::get)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .combinedClickable(onClick = {}, onLongClick = { onDelete(entry) }),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(category?.emoji ?: "💸", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = entry.merchant ?: category?.name ?: "Expense",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            if (entry.note != null) {
                                Text(
                                    text = entry.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = Money(entry.amountMinor, KCurrency.valueOf(entry.currency)).format(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}
