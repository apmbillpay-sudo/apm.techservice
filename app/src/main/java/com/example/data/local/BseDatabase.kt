package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WatchlistEntity::class, SavedProjectionScenario::class],
    version = 1,
    exportSchema = false
)
abstract class BseDatabase : RoomDatabase() {
    abstract fun bseDao(): BseDao

    companion object {
        @Volatile
        private var INSTANCE: BseDatabase? = null

        fun getDatabase(context: Context): BseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BseDatabase::class.java,
                    "bse_pulse_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
