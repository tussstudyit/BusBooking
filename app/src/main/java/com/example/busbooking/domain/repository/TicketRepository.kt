package com.example.busbooking.domain.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.busbooking.data.dao.SeatDAO
import com.example.busbooking.data.dao.TicketDAO
import com.example.busbooking.data.entity.Ticket
import com.example.busbooking.data.relations.TicketDetails
import com.example.busbooking.domain.models.BookingResult
import com.example.busbooking.domain.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TicketRepository(
    private val ticketDAO: TicketDAO,
    private val seatDAO: SeatDAO
) {

    /**
     * Book a ticket for a seat on a trip
     * 
     * Uses transaction to prevent double-booking
     */
    suspend fun bookTicket(
        userId: Long,
        tripId: Long,
        seatId: Long
    ): BookingResult = withContext(Dispatchers.IO) {

        try {
            // Verify seat exists
            seatDAO.getSeatById(seatId)
                ?: return@withContext BookingResult.InvalidSeat

            val isBooked = ticketDAO.isSeatsBookedForTrip(tripId, seatId)
            if (isBooked) return@withContext BookingResult.AlreadyBooked

            val ticket = Ticket(
                userId = userId,
                tripId = tripId,
                seatId = seatId,
                bookingTime = System.currentTimeMillis(),
                status = "CONFIRMED"
            )

            val id = ticketDAO.bookTicket(ticket)

            if (id > 0) {
                BookingResult.Success(id)
            } else {
                BookingResult.Failure(Exception("Booking failed"))
            }

        } catch (e: SQLiteConstraintException) {
            BookingResult.AlreadyBooked
        } catch (e: Exception) {
            BookingResult.Failure(e)
        }
    }

    /**
     * Get ticket by ID with full details
     */
    suspend fun getTicketById(ticketId: Long): Result<TicketDetails> = 
        withContext(Dispatchers.IO) {
            try {
                val ticket = ticketDAO.getTicketById(ticketId)

                if (ticket != null) {
                    Result.Success(ticket)
                } else {
                    Result.Error(Exception("Not found"), "Ticket not found")
                }

            } catch (e: Exception) {
                Result.Error(e, "Error fetching ticket: ${e.message}")
            }
        }

    /**
     * Get user's active tickets (CONFIRMED or PENDING)
     */
    suspend fun getUserActiveTickets(userId: Long): Result<List<TicketDetails>> = 
        withContext(Dispatchers.IO) {
            try {
                val tickets = ticketDAO.getUserActiveTickets(userId)
                Result.Success(tickets)
            } catch (e: Exception) {
                Result.Error(e, "Error fetching tickets: ${e.message}")
            }
        }

    /**
     * Cancel ticket with refund
     */
    suspend fun cancelTicket(
        ticketId: Long,
        userId: Long,
        reason: String,
        refundAmount: Double
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val rowsAffected = ticketDAO.cancelTicket(
                ticketId = ticketId,
                userId = userId,
                reason = reason,
                refundAmount = refundAmount
            )

            if (rowsAffected > 0) {
                Result.Success(rowsAffected)
            } else {
                Result.Error(
                    Exception("Cancel failed"), 
                    "Could not cancel ticket. It may not exist or be in invalid status"
                )
            }

        } catch (e: Exception) {
            Result.Error(e, "Error cancelling ticket: ${e.message}")
        }
    }

    /**
     * Mark ticket as used
     */
    suspend fun markTicketAsUsed(ticketId: Long): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                ticketDAO.markTicketAsUsed(ticketId)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, "Error marking ticket as used: ${e.message}")
            }
        }

    /**
     * Get tickets for a trip
     */
    suspend fun getTicketsForTrip(tripId: Long): Result<List<TicketDetails>> = 
        withContext(Dispatchers.IO) {
            try {
                var tickets: List<TicketDetails> = emptyList()
                ticketDAO.getTicketsForTrip(tripId).collect { list ->
                    tickets = list
                }
                Result.Success(tickets)
            } catch (e: Exception) {
                Result.Error(e, "Error fetching trip tickets: ${e.message}")
            }
        }

    /**
     * Check if seat is already booked for trip
     */
    suspend fun isSeatsBookedForTrip(tripId: Long, seatId: Long): Result<Boolean> = 
        withContext(Dispatchers.IO) {
            try {
                val isBooked = ticketDAO.isSeatsBookedForTrip(tripId, seatId)
                Result.Success(isBooked)
            } catch (e: Exception) {
                Result.Error(e, "Error checking seat booking: ${e.message}")
            }
        }
}