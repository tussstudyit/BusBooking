package com.example.busbooking.domain.repository

import com.example.busbooking.data.relations.TicketDetails
import com.example.busbooking.domain.models.Result

data class PendingPaymentSession(val paymentId: String)

class ApiTicketRepository(private val ticketRepository: TicketRepository = TicketRepository()) {
    suspend fun getTicketById(ticketId: Long): Result<TicketDetails> = ticketRepository.getTicketById(ticketId)
    suspend fun getUserActiveTickets(userId: Long): Result<List<TicketDetails>> = ticketRepository.getUserActiveTickets(userId)
    suspend fun getUserTicketHistory(userId: Long): Result<List<TicketDetails>> = ticketRepository.getUserTicketHistory(userId)
    suspend fun getPendingPaymentSession(ticketId: Long): Result<PendingPaymentSession> {
        return when (val ticket = ticketRepository.getTicketById(ticketId)) {
            is Result.Success -> ticket.data.ticket.paymentId?.let { Result.Success(PendingPaymentSession(it)) }
                ?: Result.Error(Exception("Missing payment"), "KhÃ´ng tÃ¬m tháº¥y phiÃªn thanh toÃ¡n")
            is Result.Error -> ticket
            Result.Loading -> Result.Loading
        }
    }
}

