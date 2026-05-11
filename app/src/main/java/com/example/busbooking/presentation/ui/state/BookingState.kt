package com.example.busbooking.presentation.ui.state

import com.example.busbooking.data.entity.Seat
import com.example.busbooking.data.relations.TicketDetails

sealed class BookingState {
    object Idle : BookingState()
    object Loading : BookingState()
    data class SeatsLoaded(val seats: List<Seat>) : BookingState()
    data class BookingSuccess(val ticketId: Long) : BookingState()
    data class TicketLoaded(val ticket: TicketDetails) : BookingState()
    data class CancelSuccess(val message: String = "Đã hủy vé thành công") : BookingState()
    data class Error(val message: String) : BookingState()
}