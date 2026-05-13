package com.example.busbooking.domain.repository

import com.example.busbooking.data.dao.SeatDAO
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.domain.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SeatRepository(private val seatDAO: SeatDAO) {

    /**
     * Get all seats for a trip (with booking status)
     */
    suspend fun getSeatsByTripId(tripId: Long): Result<List<Seat>> =
        withContext(Dispatchers.IO) {
            try {
                val seats = seatDAO.getSeatsByTripId(tripId)
                Result.Success(seats)
            } catch (e: Exception) {
                Result.Error(e, "Error fetching seats: ${e.message}")
            }
        }

    /**
     * Get free/available seats for a trip
     */
    suspend fun getFreeSeatsForTrip(tripId: Long): Result<List<Seat>> =
        withContext(Dispatchers.IO) {
            try {
                val seats = seatDAO.getFreeSeatsForTrip(tripId)
                Result.Success(seats)
            } catch (e: Exception) {
                Result.Error(e, "Error fetching free seats: ${e.message}")
            }
        }

    /**
     * Get booked seats for a trip
     */
    suspend fun getBookedSeatsForTrip(tripId: Long): Result<List<Seat>> =
        withContext(Dispatchers.IO) {
            try {
                val seats = seatDAO.getBookedSeatsForTrip(tripId)
                Result.Success(seats)
            } catch (e: Exception) {
                Result.Error(e, "Error fetching booked seats: ${e.message}")
            }
        }

    /**
     * Get seat by ID
     */
    suspend fun getSeatById(seatId: Long): Result<Seat> =
        withContext(Dispatchers.IO) {
            try {
                val seat = seatDAO.getSeatById(seatId)

                if (seat != null) {
                    Result.Success(seat)
                } else {
                    Result.Error(Exception("Not found"), "Seat not found")
                }

            } catch (e: Exception) {
                Result.Error(e, "Error fetching seat: ${e.message}")
            }
        }

    /**
     * Get seat by bus and seat number
     */
    suspend fun getSeatByBusAndNumber(busId: Long, seatNumber: String): Result<Seat> =
        withContext(Dispatchers.IO) {
            try {
                val seat = seatDAO.getSeatByBusAndNumber(busId, seatNumber)

                if (seat != null) {
                    Result.Success(seat)
                } else {
                    Result.Error(Exception("Not found"), "Seat not found")
                }

            } catch (e: Exception) {
                Result.Error(e, "Error fetching seat: ${e.message}")
            }
        }

    /**
     * Get booked seat count for a trip
     */
    suspend fun getBookedSeatCountForTrip(tripId: Long): Result<Long> =
        withContext(Dispatchers.IO) {
            try {
                val count = seatDAO.getBookedSeatCountForTrip(tripId)
                Result.Success(count)
            } catch (e: Exception) {
                Result.Error(e, "Error counting booked seats: ${e.message}")
            }
        }

    /**
     * Insert seat
     */
    suspend fun insertSeat(seat: Seat): Result<Long> =
        withContext(Dispatchers.IO) {
            try {
                val id = seatDAO.insertSeat(seat)

                if (id > 0) {
                    Result.Success(id)
                } else {
                    Result.Error(Exception("Insert failed"), "Could not create seat")
                }

            } catch (e: Exception) {
                Result.Error(e, "Error creating seat: ${e.message}")
            }
        }

    /**
     * Insert multiple seats
     */
    suspend fun insertSeats(seats: List<Seat>): Result<List<Long>> =
        withContext(Dispatchers.IO) {
            try {
                val ids = seatDAO.insertSeats(seats)
                Result.Success(ids)
            } catch (e: Exception) {
                Result.Error(e, "Error creating seats: ${e.message}")
            }
        }
    suspend fun getSeatsByBusId(busId: Long): Result<List<Seat>> =
        withContext(Dispatchers.IO) {
            try {
                val seats = seatDAO.getSeatsByBusId(busId)
                Result.Success(seats)
            } catch (e: Exception) {
                Result.Error(e, "Error fetching seats by bus: ${e.message}")
            }
        }

    suspend fun generateSeats(busId: Long, totalSeats: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val cols = listOf("A","B","C","D","E","F","G","H","I","J","K","L","M","N","O")
                val seats = (1..totalSeats).map { i ->
                    val row = cols[(i - 1) / 4]
                    val col = ((i - 1) % 4)
                    val rowIndex = (i - 1) / 4
                    val columnIndex = col
                    Seat(
                        busId = busId,
                        seatNumber = "$row${col + 1}",
                        floor = if (i <= totalSeats / 2) 1 else 2,
                        rowIndex = rowIndex,
                        columnIndex = columnIndex,
                        isWindow = (col == 0 || col == 3),
                        seatType = "NORMAL"
                    )
                }
                seatDAO.insertSeats(seats)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, "Error generating seats: ${e.message}")
            }
        }
}