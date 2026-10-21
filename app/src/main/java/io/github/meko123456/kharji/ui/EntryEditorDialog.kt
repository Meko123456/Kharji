package io.github.meko123456.kharji.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import io.github.meko123456.kharji.data.Category
import io.github.meko123456.kharji.domain.KCurrency

/** Quick-add expense: amount, currency chips, category chips, optional merchant/note. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryEditorDialog(
    categories: List<Category>,
    onSave: (amount: String, currency: KCurrency, categoryId: Long?, merchant: String?, note: String?) -> Boolean,
    onDismiss: () -> Unit,
    initialAmount: String = "",
    initialCurrency: KCurrency = KCurrency.GEL,
    initialMerchant: String = "",
) {
    var amount by rememberSaveable { mutableStateOf(initialAmount) }
    var currency by rememberSaveable { mutableStateOf(initialCurrency) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var merchant by rememberSaveable { mutableStateOf(initialMerchant) }
    var amountError by rememberSaveable { mutableStateOf(false) }
    val save = {
        val ok = onSave(amount, currency, categoryId, merchant, null)
        if (!ok) amountError = true else onDismiss()
    }
    // The keyboard's action key goes from the amount to the merchant, past the chips, then saves.
    val merchantFocus = remember { FocusRequester() }
    val amountFocus = remember { FocusRequester() }

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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { merchantFocus.requestFocus() }),
                    modifier = Modifier.focusRequester(amountFocus),
                )
                // Wraps rather than squeezing: in one row at 2x text on a 360dp phone, EUR was
                // pressed into a sliver with one letter to a line and could not be picked.
                FlowRow(
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.padding(top = 8.dp).focusRequester(merchantFocus),
                )
            }
            // With no amount yet (+, the Add an expense shortcut, a merchant picked out of another
            // app), the amount is the next thing to type, so it has the number keyboard at once.
            LaunchedEffect(Unit) { if (initialAmount.isEmpty()) amountFocus.requestFocus() }
        },
        confirmButton = {
            TextButton(onClick = save) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
