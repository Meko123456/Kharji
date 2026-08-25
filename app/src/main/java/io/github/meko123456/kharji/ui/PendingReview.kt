package io.github.meko123456.kharji.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.meko123456.kharji.data.Entry
import io.github.meko123456.kharji.domain.KCurrency
import io.github.meko123456.kharji.domain.Money

/**
 * Review strip for bank captures awaiting confirmation. Captures are deliberately
 * never counted until the user accepts them, so a misparse can't silently distort totals.
 */
@Composable
fun PendingReview(
    pending: List<Entry>,
    onConfirm: (Entry) -> Unit,
    onDiscard: (Entry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pending.isEmpty()) return

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "📥 ${pending.size} captured — confirm to count",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            pending.forEach { entry ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.padding(end = 8.dp)) {
                        Text(formatAmount(entry), style = MaterialTheme.typography.bodyLarge)
                        entry.merchant?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row {
                        TextButton(onClick = { onDiscard(entry) }) { Text("Discard") }
                        TextButton(onClick = { onConfirm(entry) }) { Text("Confirm") }
                    }
                }
            }
        }
    }
}

private fun formatAmount(entry: Entry): String {
    val currency = KCurrency.entries.firstOrNull { it.code == entry.currency } ?: KCurrency.GEL
    return Money(entry.amountMinor, currency).format()
}
