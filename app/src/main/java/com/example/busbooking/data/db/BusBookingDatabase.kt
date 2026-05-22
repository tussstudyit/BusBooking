package com.example.busbooking.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.busbooking.data.dao.*
import com.example.busbooking.data.entity.*

@Database(
    entities = [User::class, Route::class, Bus::class, Trip::class, Seat::class, Ticket::class],
    version = 4,
    exportSchema = false
)
abstract class BusBookingDatabase : RoomDatabase() {

    abstract fun userDao(): UserDAO
    abstract fun routeDao(): RouteDAO
    abstract fun busDao(): BusDAO
    abstract fun tripDao(): TripDAO
    abstract fun seatDao(): SeatDAO
    abstract fun ticketDao(): TicketDAO

    companion object {
        @Volatile
        private var instance: BusBookingDatabase? = null

        fun getInstance(context: Context): BusBookingDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    BusBookingDatabase::class.java,
                    "bus_booking.db"
                )
                    .fallbackToDestructiveMigration()  // ← bỏ (true) đi
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            SeedDataProvider.seedDatabase(db)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)

                            // Ensure users table has admin account
                            val userCursor = db.query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'", arrayOf<Any?>())
                            userCursor.moveToFirst()
                            val adminCount = userCursor.getLong(0)
                            userCursor.close()

                            if (adminCount == 0L) {
                                // No admin found, seed admin account
                                SeedDataProvider.seedUsers(db)
                            }
                        }
                    })
                    .build()
                    .also { instance = it }
            }
        }
    }
}
