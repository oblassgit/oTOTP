package com.example.ototp.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TOTPTokenEntity::class], // Add other entities here if needed
    version = 1, // Increment this if you change your schema
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao

    companion object {
        const val DB_NAME = "oTOTP_db"
    }
}