package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.Seat
import com.example.busbooking.domain.models.Result

class SeatRepository {
    suspend fun getSeatsByTripId(tripId: Long): Result<List<Seat>> = api { ApiClient.get<List<SeatApi>>("/api/mobile/trips/$tripId/seats").map { it.toEntity() } }
    suspend fun getFreeSeatsForTrip(tripId: Long): Result<List<Seat>> = api { ApiClient.get<List<SeatApi>>("/api/mobile/trips/$tripId/seats").filter { !it.booked }.map { it.toEntity() } }
    suspend fun getBookedSeatsForTrip(tripId: Long): Result<List<Seat>> = api { ApiClient.get<List<SeatApi>>("/api/mobile/trips/$tripId/seats").filter { it.booked }.map { it.toEntity() } }
    suspend fun getSeatById(seatId: Long): Result<Seat> = Result.Error(UnsupportedOperationException(), "KhÃ´ng há»— trá»£ táº£i gháº¿ riÃªng láº»")
    suspend fun getSeatByBusAndNumber(busId: Long, seatNumber: String): Result<Seat> = Result.Error(UnsupportedOperationException(), "KhÃ´ng há»— trá»£ táº£i gháº¿ riÃªng láº»")
    suspend fun getBookedSeatCountForTrip(tripId: Long): Result<Long> = api { ApiClient.get<SeatAvailabilityApi>("/api/mobile/trips/$tripId/availability").bookedSeats.toLong() }
    suspend fun insertSeat(seat: Seat): Result<Long> = Result.Error(UnsupportedOperationException(), "Quáº£n trá»‹ gháº¿ thá»±c hiá»‡n trÃªn web admin")
    suspend fun insertSeats(seats: List<Seat>): Result<List<Long>> = Result.Error(UnsupportedOperationException(), "Quáº£n trá»‹ gháº¿ thá»±c hiá»‡n trÃªn web admin")
    suspend fun getSeatsByBusId(busId: Long): Result<List<Seat>> = Result.Error(UnsupportedOperationException(), "KhÃ´ng há»— trá»£ theo bus")
    suspend fun generateSeats(busId: Long, totalSeats: Int): Result<Unit> = Result.Error(UnsupportedOperationException(), "Quáº£n trá»‹ gháº¿ thá»±c hiá»‡n trÃªn web admin")
    private suspend fun <T> api(block: suspend () -> T): Result<T> = try { Result.Success(block()) } catch (e: Exception) { Result.Error(e, e.message ?: "Lá»—i káº¿t ná»‘i API") }
}

