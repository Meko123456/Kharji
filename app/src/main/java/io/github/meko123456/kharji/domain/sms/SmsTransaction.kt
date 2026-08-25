package io.github.meko123456.kharji.domain.sms

import io.github.meko123456.kharji.data.EntrySource
import io.github.meko123456.kharji.domain.KCurrency

/**
 * A transaction extracted from a bank SMS, before the user confirms it.
 *
 * Deliberately free of Android and Room types so parsing is pure and unit-testable;
 * the ViewModel maps this into a pending [io.github.meko123456.kharji.data.Entry].
 */
data class SmsTransaction(
    val amountMinor: Long,
    val currency: KCurrency,
    val merchant: String?,
    val source: EntrySource,
    /** True for money leaving the account (a purchase/withdrawal) — what Kharji tracks. */
    val isDebit: Boolean,
)
