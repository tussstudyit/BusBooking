package com.example.busbooking.data.dao

import androidx.room.*
import com.example.busbooking.data.entity.Seat
import kotlinx.coroutines.flow.Flow

@Dao
interface SeatDAO {

    // ================= INSERT =================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSeats(seats: List<Seat>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSeat(seat: Seat): Long

    @Update
    suspend fun updateSeat(seat: Seat)

    @Query("DELETE FROM seats WHERE id = :seatId")
    suspend fun deleteSeat(seatId: Long)

    @Query("DELETE FROM seats WHERE busId = :busId")
    suspend fun deleteSeatsForBus(busId: Long)

    // ================= BASIC =================

    @Query("""
        SELECT * FROM seats 
        WHERE busId = :busId 
        ORDER BY seatNumber
    """)
    suspend fun getSeatsByBus(busId: Long): List<Seat>

    @Query("""
        SELECT * FROM seats 
        WHERE busId = :busId 
        ORDER BY seatNumber
    """)
    fun getSeatsByBusFlow(busId: Long): Flow<List<Seat>>

    @Query("SELECT * FROM seats WHERE id = :seatId")
    suspend fun getSeatById(seatId: Long): Seat?

    @Query("""
        SELECT * FROM seats 
        WHERE busId = :busId AND seatNumber = :seatNumber 
        LIMIT 1
    """)
    suspend fun getSeatByBusAndNumber(busId: Long, seatNumber: String): Seat?

    // ================= BOOKING LOGIC =================

    /**
     * 🔥 Ghế trống theo TRIP (QUAN TRỌNG NHẤT)
     */
    @Query("""
        SELECT s.* FROM seats s
        WHERE s.busId = (SELECT busId FROM trips WHERE id = :tripId)
          AND NOT EXISTS (
            SELECT 1 FROM tickets t
            WHERE t.tripId = :tripId 
              AND t.seatId = s.id 
              AND t.status IN ('CONFIRMED', 'PENDING')
          )
        ORDER BY s.seatNumber
    """)
    suspend fun getFreeSeatsForTrip(tripId: Long): List<Seat>

    /**
     * Ghế đã đặt theo trip
     */
    @Query("""
        SELECT s.* FROM seats s
        WHERE s.busId = (SELECT busId FROM trips WHERE id = :tripId)
          AND EXISTS (
            SELECT 1 FROM tickets t
            WHERE t.tripId = :tripId 
              AND t.seatId = s.id 
              AND t.status IN ('CONFIRMED', 'PENDING')
          )
        ORDER BY s.seatNumber
    """)
    suspend fun getBookedSeatsForTrip(tripId: Long): List<Seat>

    // ================= STATS =================

    @Query("SELECT COUNT(*) FROM seats WHERE busId = :busId")
    suspend fun getSeatCountForBus(busId: Long): Long

    @Query("""
        SELECT COUNT(DISTINCT t.seatId) FROM tickets t
        WHERE t.tripId = :tripId 
          AND t.status IN ('CONFIRMED', 'PENDING')
    """)
    suspend fun getBookedSeatCountForTrip(tripId: Long): Long

    @Query("""
    SELECT s.* FROM seats s
    INNER JOIN trips t ON s.busId = t.busId
    WHERE t.id = :tripId
    ORDER BY s.seatNumber
""")
    suspend fun getSeatsByTripId(tripId: Long): List<Seat>
}