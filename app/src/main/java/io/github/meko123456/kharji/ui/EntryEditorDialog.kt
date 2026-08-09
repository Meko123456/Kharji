package io.github.meko123456.kharji.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import io.github.meko123456.kharji.data.Category
import io.github.meko123456.kharji.domain.KCurrency

/** Quick-add expense: amount, currency chips, category chips, optional merchant/note. */
@Composable
fun EntryEditorDialog(
    categories: List<Category>,
    onSave: (amount: String, currency: KCurrency, categoryId: Long?, merchant: String?, note: String?) -> Boolean,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(KCurrency.GEL) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var merchant by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New expense") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; amountError = false },
                    label = { Text(if (amountError) "Enter a valid amount" else "Amount") },
                    isError = amountError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    KCurrency.entries.forEach { c ->
                        FilterChip(
                            selected = currency == c,
                            onClick = { currency = c },
                            label = { Text(c.code) },
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = categoryId == cat.id,
                            onClick = { categoryId = if (categoryId == cat.id) null else cat.id },
                            label = { Text("${cat.emoji} ${cat.name}") },
                        )
                    }
                }
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / note (optional)") },
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ok = onSave(amount, currency, categoryId, merchant, null)
                if (!ok) amountError = true else onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
