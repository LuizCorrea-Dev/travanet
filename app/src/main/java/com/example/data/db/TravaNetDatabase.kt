package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BlockingProfileEntity::class, ScheduleRuleEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TravaNetDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var instance: TravaNetDatabase? = null

        fun getInstance(context: Context): TravaNetDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TravaNetDatabase::class.java,
                    "travanet_db",
                ).build().also { instance = it }
            }
        }
    }
}
