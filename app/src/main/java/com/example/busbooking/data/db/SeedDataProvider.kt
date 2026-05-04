package com.example.busbooking.data.db

import androidx.sqlite.db.SupportSQLiteDatabase

object SeedDataProvider {

    fun seedDatabase(db: SupportSQLiteDatabase) {
        try {
            seedUsers(db)
            seedRoutes(db)
            seedBuses(db)
            seedSeats(db)
            seedTrips(db)
            seedTickets(db)

            println("✅ Database seeded successfully!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun seedUsers(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO users (id, name, email, password, phone, role) VALUES (1, 'Admin', 'admin@bus.com', '123', '0123', 'ADMIN')")
        db.execSQL("INSERT INTO users (id, name, email, password, phone, role) VALUES (2, 'User A', 'user@gmail.com', '123', '0456', 'USER')")
    }

    private fun seedRoutes(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO routes (id, origin, destination, distance, isActive) VALUES (1, 'Da Nang', 'Hue', 100, 1)")
        db.execSQL("INSERT INTO routes (id, origin, destination, distance, isActive) VALUES (2, 'Da Nang', 'Hoi An', 30, 1)")
    }

    private fun seedBuses(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO buses (id, busName, totalSeats, licensePlate, isActive) VALUES (1, 'Bus A', 20, '43A-12345', 1)")
        db.execSQL("INSERT INTO buses (id, busName, totalSeats, licensePlate, isActive) VALUES (2, 'Bus B', 20, '43B-67890', 1)")
    }

    private fun seedSeats(db: SupportSQLiteDatabase) {
        var seatId = 1

        for (i in 1..20) {
            db.execSQL("INSERT INTO seats (id, busId, seatNumber) VALUES ($seatId, 1, 'A$i')")
            seatId++
        }

        for (i in 1..20) {
            db.execSQL("INSERT INTO seats (id, busId, seatNumber) VALUES ($seatId, 2, 'A$i')")
            seatId++
        }
    }

    private fun seedTrips(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        val oneDay = 86400000

        db.execSQL("INSERT INTO trips (id, routeId, busId, departureTime, arrivalTime, price, tripDate, status) VALUES (1, 1, 1, $now, ${now + oneDay}, 100.0, $now, 'SCHEDULED')")
        db.execSQL("INSERT INTO trips (id, routeId, busId, departureTime, arrivalTime, price, tripDate, status) VALUES (2, 2, 2, $now, ${now + oneDay}, 150.0, $now, 'SCHEDULED')")
    }

    private fun seedTickets(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        db.execSQL("INSERT INTO tickets (id, userId, tripId, seatId, bookingTime, status) VALUES (1, 2, 1, 1, $now, 'CONFIRMED')")
        db.execSQL("INSERT INTO tickets (id, userId, tripId, seatId, bookingTime, status) VALUES (2, 2, 1, 2, $now, 'CONFIRMED')")
    }
}