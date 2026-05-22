package com.example.busbooking.domain.models

sealed class SeatReservationResult {
    data class Success(
        val ticketIds: List<Long>,
        val paymentId: String
    ) : SeatReservationResult()
    data class AlreadyTaken(val seatNumber: String) : SeatReservationResult()
    data class Failure(val message: String) : SeatReservationResult()
}
