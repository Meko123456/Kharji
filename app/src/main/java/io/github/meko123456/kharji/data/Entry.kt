package io.github.meko123456.kharji.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Where an entry came from. SMS-sourced entries start [Entry.pending] = true. */
enum class EntrySource { MANUAL, SMS_TBC, SMS_BOG }

/**
 * One expense. Amount is stored in minor units in the ORIGINAL currency —
 * conversion happens at display time via cached [FxRate]s, never at write time.
 */
@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId"), Index("epochDay")],
)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val currency: String,          // KCurrency.code
    val categoryId: Long? = null,
    val merchant: String? = null,
    val note: String? = null,
    val epochDay: Long,            // transaction date
    val createdAtMillis: Long,     // record creation time
    val source: EntrySource = EntrySource.MANUAL,
    val pending: Boolean = false,  // SMS captures await user confirmation
)
