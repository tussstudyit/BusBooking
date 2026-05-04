package com.example.busbooking.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase

class DatabaseSeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        SeedDataProvider.seedDatabase(db)
    }
}