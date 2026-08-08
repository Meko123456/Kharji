package io.github.meko123456.kharji.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Entry::class, Category::class, FxRate::class],
    version = 1,
    exportSchema = false,
)
abstract class KharjiDatabase : RoomDatabase() {

    abstract fun dao(): KharjiDao

    companion object {
        @Volatile
        private var instance: KharjiDatabase? = null

        fun get(context: Context): KharjiDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KharjiDatabase::class.java,
                    "kharji.db",
                ).build().also { instance = it }
            }
    }
}
