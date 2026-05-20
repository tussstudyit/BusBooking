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

                            db.execSQL("PRAGMA foreign_keys = OFF")

                            // Ensure users table has admin account
                            val userCursor = db.query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'", arrayOf<Any?>())
                            userCursor.moveToFirst()
                            val adminCount = userCursor.getLong(0)
                            userCursor.close()

                            if (adminCount == 0L) {
                                // No admin found, seed admin account
                                SeedDataProvider.seedUsers(db)
                            }

                            val cursor = db.query("SELECT id, origin, destination FROM routes", arrayOf<Any?>())
                            while (cursor.moveToNext()) {
                                android.util.Log.d("DB_CHECK", "Route: id=${cursor.getLong(0)}, origin=${cursor.getString(1)}, destination=${cursor.getString(2)}")
                            }
                            cursor.close()


                            db.execSQL("DELETE FROM tickets")
                            db.execSQL("DELETE FROM trips")
                            db.execSQL("DELETE FROM seats")
                            db.execSQL("DELETE FROM routes")
                            db.execSQL("DELETE FROM buses")

                            val busIds = SeedDataProvider.seedBuses(db)
                            val routeIds = SeedDataProvider.seedRoutes(db)
                            SeedDataProvider.seedSeats(db, busIds)
                            SeedDataProvider.seedTrips(db, routeIds, busIds)

                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    })
                    .build()
                    .also { instance = it }
            }
        }
    }
}