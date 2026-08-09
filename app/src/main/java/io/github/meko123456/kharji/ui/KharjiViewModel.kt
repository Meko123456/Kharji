package io.github.meko123456.kharji.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.meko123456.kharji.data.Category
import io.github.meko123456.kharji.data.Entry
import io.github.meko123456.kharji.data.EntrySource
import io.github.meko123456.kharji.data.KharjiDao
import io.github.meko123456.kharji.data.KharjiDatabase
import io.github.meko123456.kharji.domain.KCurrency
import io.github.meko123456.kharji.domain.Money
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KharjiViewModel(private val dao: KharjiDao) : ViewModel() {

    val entries: StateFlow<List<Entry>> =
        dao.observeEntries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> =
        dao.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (dao.categoryCount() == 0) {
                DEFAULT_CATEGORIES.forEach { dao.insert(it) }
            }
        }
    }

    /** Returns false when the amount doesn't parse. */
    fun addEntry(
        amount: String,
        currency: KCurrency,
        categoryId: Long?,
        merchant: String?,
        note: String?,
    ): Boolean {
        val money = runCatching { Money.of(amount, currency) }.getOrNull() ?: return false
        if (money.minor <= 0) return false
        viewModelScope.launch {
            dao.insert(
                Entry(
                    amountMinor = money.minor,
                    currency = currency.code,
                    categoryId = categoryId,
                    merchant = merchant?.trim()?.ifEmpty { null },
                    note = note?.trim()?.ifEmpty { null },
                    epochDay = LocalDate.now().toEpochDay(),
                    createdAtMillis = System.currentTimeMillis(),
                    source = EntrySource.MANUAL,
                ),
            )
        }
        return true
    }

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch { dao.delete(entry) }
    }

    companion object {
        private val DEFAULT_CATEGORIES = listOf(
            Category(name = "Food", emoji = "🍔"),
            Category(name = "Groceries", emoji = "🛒"),
            Category(name = "Transport", emoji = "🚕"),
            Category(name = "Bills", emoji = "🧾"),
            Category(name = "Fun", emoji = "🎉"),
            Category(name = "Other", emoji = "💸"),
        )

        val Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[APPLICATION_KEY]) as Application
                @Suppress("UNCHECKED_CAST")
                return KharjiViewModel(KharjiDatabase.get(app).dao()) as T
            }
        }
    }
}
