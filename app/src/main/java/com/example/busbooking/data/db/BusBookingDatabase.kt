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
    version = 2, // 🔥 tăng version vì đã sửa entity
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
                    .fallbackToDestructiveMigration() // ✔ demo OK

                    // 🔥 GẮN SEED DATA
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            SeedDataProvider.seedDatabase(db)
                        }
                    })

                    .build()
                    .also { instance = it }
            }
        }
    }
}