package com.example.busbooking.domain.repository

import com.example.busbooking.data.relations.TripWithRouteAndBus
import com.example.busbooking.domain.models.Result
import java.net.URLEncoder
import java.util.Calendar

data class TripSeatAvailability(val totalSeats: Int, val bookedSeats: Int) {
    val availableSeats: Int get() = (totalSeats - bookedSeats).coerceAtLeast(0)
    val occupiedPercent: Int get() = if (totalSeats <= 0) 0 else (bookedSeats.coerceIn(0, totalSeats) * 100 / totalSeats)
}

interface ITripRepository {
    suspend fun searchTrips(origin: String, destination: String, tripDate: Long, totalSeats: Int? = null): Result<List<TripWithRouteAndBus>>
    suspend fun getUpcomingTripsForRoute(routeId: Long, fromDate: Long): Result<List<TripWithRouteAndBus>>
    suspend fun getTripById(tripId: Long): Result<TripWithRouteAndBus>
    suspend fun getSeatAvailability(tripId: Long, busId: Long): Result<TripSeatAvailability>
    suspend fun getAvailableSeatsCount(tripId: Long, busId: Long): Result<Int>
    suspend fun createTrip(routeId: Long, busId: Long, departureTime: Long, arrivalTime: Long, price: Double, tripDate: Long): Result<Long>
    suspend fun cancelTrip(tripId: Long): Result<Unit>
    suspend fun getAllTrips(): Result<List<TripWithRouteAndBus>>
}

class TripRepository : ITripRepository {
    override suspend fun searchTrips(origin: String, destination: String, tripDate: Long, totalSeats: Int?): Result<List<TripWithRouteAndBus>> = api {
        val dayStart = Calendar.getInstance().apply { timeInMillis = tripDate; set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }.timeInMillis
        val seatParam = totalSeats?.takeIf { it == 24 || it == 34 }?.let { "&totalSeats=$it" }.orEmpty()
        val trips: List<TripApi> = ApiClient.get("/api/mobile/trips/search?origin=${enc(origin)}&destination=${enc(destination)}&tripDate=$dayStart$seatParam")
        trips.map { it.toRelation() }
    }

    override suspend fun getTripById(tripId: Long): Result<TripWithRouteAndBus> = api { ApiClient.get<TripApi>("/api/mobile/trips/$tripId").toRelation() }
    override suspend fun getSeatAvailability(tripId: Long, busId: Long): Result<TripSeatAvailability> = api { ApiClient.get<SeatAvailabilityApi>("/api/mobile/trips/$tripId/availability").let { TripSeatAvailability(it.totalSeats, it.bookedSeats) } }
    override suspend fun getAvailableSeatsCount(tripId: Long, busId: Long): Result<Int> = when (val result = getSeatAvailability(tripId, busId)) { is Result.Success -> Result.Success(result.data.availableSeats); is Result.Error -> result; Result.Loading -> Result.Loading }
    override suspend fun getUpcomingTripsForRoute(routeId: Long, fromDate: Long): Result<List<TripWithRouteAndBus>> = Result.Error(UnsupportedOperationException(), "TÃ¬m chuyáº¿n theo tuyáº¿n chÆ°a dÃ¹ng trong API mobile")
    override suspend fun createTrip(routeId: Long, busId: Long, departureTime: Long, arrivalTime: Long, price: Double, tripDate: Long): Result<Long> = Result.Error(UnsupportedOperationException(), "Táº¡o chuyáº¿n thá»±c hiá»‡n trÃªn web admin")
    override suspend fun cancelTrip(tripId: Long): Result<Unit> = Result.Error(UnsupportedOperationException(), "Há»§y chuyáº¿n thá»±c hiá»‡n trÃªn web admin")
    override suspend fun getAllTrips(): Result<List<TripWithRouteAndBus>> = Result.Error(UnsupportedOperationException(), "Danh sÃ¡ch táº¥t cáº£ chuyáº¿n dÃ nh cho web admin")

    private suspend fun <T> api(block: suspend () -> T): Result<T> = try { Result.Success(block()) } catch (e: Exception) { Result.Error(e, e.message ?: "Lá»—i káº¿t ná»‘i API") }
    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}

