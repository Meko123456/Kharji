package io.github.meko123456.kharji.data

import androidx.room.Entity

/** Latest cached rate per currency pair: 1 [fromCode] = [rate] [toCode]. */
@Entity(tableName = "fx_rates", primaryKeys = ["fromCode", "toCode"])
data class FxRate(
    val fromCode: String,
    val toCode: String,
    val rate: Double,
    val asOfEpochDay: Long,
)
