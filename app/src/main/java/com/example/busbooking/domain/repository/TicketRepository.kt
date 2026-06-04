package com.example.busbooking.domain.repository

import com.example.busbooking.data.relations.TicketDetails
import com.example.busbooking.data.relations.isTicketHistory
import com.example.busbooking.data.relations.isUpcomingTicket
import com.example.busbooking.domain.models.BookingResult
import com.example.busbooking.domain.models.Result

class TicketRepository {
    suspend fun bookTicket(userId: Long, tripId: Long, seatId: Long): BookingResult = try {
        val response: BookResponse = ApiClient.post("/api/mobile/tickets/book", BookRequest(userId, tripId, seatId))
        BookingResult.Success(response.ticketId)
    } catch (e: Exception) {
        if (e.message?.contains("Gháº¿", ignoreCase = true) == true) BookingResult.AlreadyBooked else BookingResult.Failure(e)
    }

    suspend fun getTicketById(ticketId: Long): Result<TicketDetails> = api { ApiClient.get<TicketDetailsApi>("/api/mobile/tickets/$ticketId").toRelation() }
    suspend fun getUserActiveTickets(userId: Long): Result<List<TicketDetails>> = api { ApiClient.get<List<TicketDetailsApi>>("/api/mobile/users/$userId/tickets").map { it.toRelation() }.filter { it.isUpcomingTicket() } }
    suspend fun getUserTicketHistory(userId: Long): Result<List<TicketDetails>> = api { ApiClient.get<List<TicketDetailsApi>>("/api/mobile/users/$userId/tickets").map { it.toRelation() }.filter { it.isTicketHistory() } }
    suspend fun cancelTicket(ticketId: Long, userId: Long, reason: String, refundAmount: Double): Result<Int> = api { ApiClient.post<CancelRequest, RowsResponse>("/api/mobile/tickets/$ticketId/cancel", CancelRequest(userId, reason, refundAmount)).rows }
    suspend fun markTicketAsUsed(ticketId: Long): Result<Unit> = Result.Error(UnsupportedOperationException(), "KhÃ´ng há»— trá»£")
    suspend fun getTicketsForTrip(tripId: Long): Result<List<TicketDetails>> = Result.Error(UnsupportedOperationException(), "KhÃ´ng há»— trá»£")
    suspend fun isSeatsBookedForTrip(tripId: Long, seatId: Long): Result<Boolean> = Result.Error(UnsupportedOperationException(), "KhÃ´ng há»— trá»£")

    private suspend fun <T> api(block: suspend () -> T): Result<T> = try { Result.Success(block()) } catch (e: Exception) { Result.Error(e, e.message ?: "Lá»—i káº¿t ná»‘i API") }
}

