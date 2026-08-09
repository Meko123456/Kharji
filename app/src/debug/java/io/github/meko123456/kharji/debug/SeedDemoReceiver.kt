package io.github.meko123456.kharji.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.meko123456.kharji.data.Category
import io.github.meko123456.kharji.data.Entry
import io.github.meko123456.kharji.data.EntrySource
import io.github.meko123456.kharji.data.FxRate
import io.github.meko123456.kharji.data.KharjiDatabase
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Debug-only: seed demo entries, categories are seeded by the app itself. */
class SeedDemoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = KharjiDatabase.get(context).dao()
                val today = LocalDate.now().toEpochDay()
                // Don't depend on the app having seeded categories yet.
                if (dao.categoryCount() == 0) {
                    listOf("Food" to "🍔", "Groceries" to "🛒", "Transport" to "🚕", "Bills" to "🧾", "Fun" to "🎉", "Other" to "💸")
                        .forEach { (name, emoji) -> dao.insert(Category(name = name, emoji = emoji)) }
                }
                val categories = dao.observeCategoriesOnce()
                fun cat(name: String) = categories.firstOrNull { it.name == name }?.id

                dao.upsert(FxRate("AED", "GEL", 0.735, today))
                dao.upsert(FxRate("USD", "GEL", 2.70, today))
                dao.upsert(FxRate("EUR", "GEL", 2.95, today))

                listOf(
                    Entry(amountMinor = 1240, currency = "GEL", categoryId = cat("Groceries"), merchant = "Spar", epochDay = today, createdAtMillis = 1, source = EntrySource.MANUAL),
                    Entry(amountMinor = 850, currency = "GEL", categoryId = cat("Transport"), merchant = "Bolt", epochDay = today, createdAtMillis = 2, source = EntrySource.MANUAL),
                    Entry(amountMinor = 4500, currency = "AED", categoryId = cat("Food"), merchant = "Careem Eats", epochDay = today - 1, createdAtMillis = 3, source = EntrySource.MANUAL),
                    Entry(amountMinor = 12900, currency = "AED", categoryId = cat("Bills"), merchant = "DEWA", epochDay = today - 1, createdAtMillis = 4, source = EntrySource.MANUAL),
                    Entry(amountMinor = 3200, currency = "GEL", categoryId = cat("Fun"), merchant = "Cinema", note = "Dune 3", epochDay = today - 2, createdAtMillis = 5, source = EntrySource.MANUAL),
                    Entry(amountMinor = 999, currency = "USD", categoryId = cat("Bills"), merchant = "Cloud storage", epochDay = today - 3, createdAtMillis = 6, source = EntrySource.MANUAL),
                ).forEach { dao.insert(it) }
            } finally {
                pending.finish()
            }
        }
    }
}
