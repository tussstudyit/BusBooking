package com.example.busbooking.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.busbooking.data.entity.Ticket
import com.example.busbooking.data.relations.TicketDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDAO {

    // ================= INSERT =================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun bookTicket(ticket: Ticket): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTicket(ticket: Ticket): Long

    /**
     * 🔥 Transaction booking (ANTI DOUBLE BOOKING)
     */
    @Transaction
    suspend fun safeBookTicket(ticket: Ticket): Long {
        val isBooked = isSeatsBookedForTrip(ticket.tripId, ticket.seatId)
        if (isBooked) return -1
        return bookTicket(ticket)
    }

    // ================= CHECK =================

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM tickets 
            WHERE tripId = :tripId AND seatId = :seatId 
              AND status IN ('CONFIRMED', 'PENDING')
        )
    """)
    suspend fun isSeatsBookedForTrip(tripId: Long, seatId: Long): Boolean

    // ================= GET DETAILS =================

    @Transaction
    @Query("SELECT * FROM tickets WHERE id = :ticketId")
    suspend fun getTicketById(ticketId: Long): TicketDetails?

    @Transaction
    @Query("""
        SELECT * FROM tickets 
        WHERE userId = :userId 
          AND status IN ('CONFIRMED', 'PENDING')
        ORDER BY bookingTime DESC
    """)
    suspend fun getUserActiveTickets(userId: Long): List<TicketDetails>

    @Transaction
    @Query("""
        SELECT * FROM tickets 
        WHERE userId = :userId
        ORDER BY bookingTime DESC
    """)
    fun getUserTicketHistory(userId: Long): Flow<List<TicketDetails>>

    @Transaction
    @Query("""
        SELECT * FROM tickets 
        WHERE userId = :userId 
          AND status IN ('CONFIRMED', 'PENDING')
        ORDER BY bookingTime DESC
        LIMIT :limit
    """)
    fun getUserRecentTickets(userId: Long, limit: Int = 5): LiveData<List<TicketDetails>>

    // ================= UPDATE =================

    /**
     * 🔥 Cancel ticket (SAFE)
     */
    @Query("""
        UPDATE tickets 
        SET status = 'CANCELLED',
            cancellationReason = :reason,
            refundAmount = :refundAmount,
            refundStatus = 'PENDING'
        WHERE id = :ticketId 
          AND userId = :userId
          AND status IN ('CONFIRMED', 'PENDING')
    """)
    suspend fun cancelTicket(
        ticketId: Long,
        userId: Long,
        reason: String,
        refundAmount: Double
    ): Int

    /**
     * 🔥 Mark used (SAFE)
     */
    @Query("""
        UPDATE tickets 
        SET status = 'USED' 
        WHERE id = :ticketId 
          AND status = 'CONFIRMED'
    """)
    suspend fun markTicketAsUsed(ticketId: Long)

    // ================= ADMIN =================

    @Transaction
    @Query("""
        SELECT * FROM tickets 
        WHERE tripId = :tripId
        ORDER BY bookingTime DESC
    """)
    fun getTicketsForTrip(tripId: Long): Flow<List<TicketDetails>>

    @Transaction
    @Query("""
        SELECT * FROM tickets 
        ORDER BY bookingTime DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getAllTickets(limit: Int = 50, offset: Int = 0): Flow<List<TicketDetails>>

    // ================= SIMPLE =================

    @Query("SELECT * FROM tickets WHERE status = :status")
    suspend fun getTicketsByStatus(status: String): List<Ticket>

    @Query("SELECT COUNT(*) FROM tickets WHERE status = :status")
    suspend fun countTicketsByStatus(status: String): Long

    @Query("""
        SELECT COUNT(*) FROM tickets 
        WHERE status = 'CONFIRMED' 
          AND bookingTime BETWEEN :startTime AND :endTime
    """)
    suspend fun countConfirmedTickets(startTime: Long, endTime: Long): Long

    // ================= DEBUG / HELPER =================

    /**
     * 🔥 Debug cực hữu ích
     */
    @Query("""
        SELECT * FROM tickets 
        WHERE tripId = :tripId AND seatId = :seatId
    """)
    suspend fun getTicketByTripAndSeat(tripId: Long, seatId: Long): Ticket?
}
