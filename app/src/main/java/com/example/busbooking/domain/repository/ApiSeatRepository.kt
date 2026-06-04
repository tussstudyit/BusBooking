package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.Seat
import com.example.busbooking.domain.models.SeatDisplay
import com.example.busbooking.domain.models.SeatReservationResult
import com.example.busbooking.domain.models.SeatStatus

data class SeatReservationSeat(val id: Long, val seatNumber: String)
data class SeatReservationSegment(val tripId: Long, val seats: List<SeatReservationSeat>, val price: Double)

class ApiSeatRepository {
    suspend fun getSeatsForTrip(tripId: Long): List<SeatDisplay> {
        val seats: List<SeatApi> = ApiClient.get("/api/mobile/trips/$tripId/seats")
        return seats.map { SeatDisplay(it.toEntity(), if (it.booked) SeatStatus.BOOKED else SeatStatus.AVAILABLE) }
    }

    suspend fun reserveSeats(
        userId: Long,
        tripId: Long,
        selectedSeats: List<Seat>,
        tripPrice: Double
    ): SeatReservationResult {
        val segment = SeatReservationSegment(
            tripId = tripId,
            seats = selectedSeats.map { SeatReservationSeat(it.id, it.seatNumber) },
            price = tripPrice
        )
        return reserveSeatSegments(userId, listOf(segment))
    }

    suspend fun reserveSeatSegments(userId: Long, segments: List<SeatReservationSegment>): SeatReservationResult {
        return try {
            val response: BookBatchResponse = ApiClient.post(
                "/api/mobile/tickets/book-batch",
                BookBatchRequest(
                    userId = userId,
                    segments = segments.map { segment ->
                        BookBatchSegment(
                            tripId = segment.tripId,
                            seats = segment.seats.map { BookBatchSeat(it.id, it.seatNumber) },
                            price = segment.price
                        )
                    }
                )
            )
            SeatReservationResult.Success(response.ticketIds, response.paymentId)
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            if (message.contains("ghe", ignoreCase = true) || message.contains("ghế", ignoreCase = true)) {
                SeatReservationResult.AlreadyTaken("")
            } else {
                SeatReservationResult.Failure(message.ifBlank { "Khong the giu ghe" })
            }
        }
    }
}
