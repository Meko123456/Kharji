package io.github.meko123456.kharji.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KharjiDao {

    // --- entries ---

    @Query("SELECT * FROM entries ORDER BY epochDay DESC, createdAtMillis DESC")
    fun observeEntries(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE pending = 1 ORDER BY createdAtMillis DESC")
    fun observePending(): Flow<List<Entry>>

    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)

    // --- categories ---

    @Query("SELECT * FROM categories ORDER BY id")
    fun observeCategories(): Flow<List<Category>>

    @Insert
    suspend fun insert(category: Category): Long

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    // --- fx rates ---

    @Query("SELECT * FROM fx_rates WHERE fromCode = :from AND toCode = :to")
    suspend fun rate(from: String, to: String): FxRate?

    @Query("SELECT * FROM fx_rates")
    fun observeRates(): Flow<List<FxRate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rate: FxRate)
}
