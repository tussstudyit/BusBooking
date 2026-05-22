package com.example.busbooking.domain.repository

import com.example.busbooking.data.dao.RouteDAO
import com.example.busbooking.data.dao.SeatDAO
import com.example.busbooking.data.dao.TripDAO
import com.example.busbooking.data.entity.Trip
import com.example.busbooking.data.relations.TripWithRouteAndBus
import com.example.busbooking.domain.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

data class TripSeatAvailability(
    val totalSeats: Int,
    val bookedSeats: Int
) {
    val availableSeats: Int
        get() = (totalSeats - bookedSeats).coerceAtLeast(0)

    val occupiedPercent: Int
        get() = if (totalSeats <= 0) {
            0
        } else {
            (bookedSeats.coerceIn(0, totalSeats) * 100 / totalSeats)
        }
}

interface ITripRepository {
    suspend fun searchTrips(origin: String, destination: String, tripDate: Long): Result<List<TripWithRouteAndBus>>
    suspend fun getUpcomingTripsForRoute(routeId: Long, fromDate: Long): Result<List<TripWithRouteAndBus>>
    suspend fun getTripById(tripId: Long): Result<TripWithRouteAndBus>
    suspend fun getSeatAvailability(tripId: Long, busId: Long): Result<TripSeatAvailability>
    suspend fun getAvailableSeatsCount(tripId: Long, busId: Long): Result<Int>
    suspend fun createTrip(
        routeId: Long,
        busId: Long,
        departureTime: Long,
        arrivalTime: Long,
        price: Double,
        tripDate: Long
    ): Result<Long>
    suspend fun cancelTrip(tripId: Long): Result<Unit>
    suspend fun getAllTrips(): Result<List<TripWithRouteAndBus>>
}

class TripRepository(
    private val tripDAO: TripDAO,
    private val routeDAO: RouteDAO,
    private val seatDAO: SeatDAO  // ✅ thêm SeatDAO
) : ITripRepository {

    /**
     * Search trips by origin, destination, and date
     */
    override suspend fun searchTrips(
        origin: String,
        destination: String,
        tripDate: Long
    ): Result<List<TripWithRouteAndBus>> = withContext(Dispatchers.IO) {
        try {
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = tripDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            android.util.Log.d("TripRepo", "origin=$origin dest=$destination dayStart=$dayStart")

            if (origin.isBlank() || destination.isBlank()) {
                return@withContext Result.Error(
                    Exception("Invalid input"),
                    "Origin/destination không được rỗng"
                )
            }

            val route = routeDAO.getRouteByOriginDestination(origin, destination)
            android.util.Log.d("TripRepo", "Route found: $route")

            if (route == null) {
                return@withContext Result.Error(
                    Exception("Route not found"),
                    "Không tìm thấy tuyến $origin → $destination"
                )
            }

            val trips = tripDAO.getTripsForRouteAndDate(route.id, dayStart)
            android.util.Log.d("TripRepo", "Trips found: ${trips.size}")

            if (trips.isNotEmpty()) {
                Result.Success(trips)
            } else {
                Result.Error(Exception("Empty"), "Không có chuyến nào ngày này")
            }

        } catch (e: Exception) {
            android.util.Log.e("TripRepo", "Error: ${e.message}")
            Result.Error(e, "Lỗi tìm kiếm: ${e.message}")
        }
    }

    /**
     * Get upcoming trips for a specific route
     */
    override suspend fun getUpcomingTripsForRoute(
        routeId: Long,
        fromDate: Long
    ): Result<List<TripWithRouteAndBus>> = withContext(Dispatchers.IO) {
        try {
            val trips = tripDAO.getUpcomingTripsForRoute(routeId, fromDate)
            if (trips.isNotEmpty()) {
                Result.Success(trips)
            } else {
                Result.Error(Exception("Empty"), "No upcoming trips")
            }
        } catch (e: Exception) {
            Result.Error(e, "Error fetching upcoming trips: ${e.message}")
        }
    }

    /**
     * Get trip by ID with full details
     */
    override suspend fun getTripById(tripId: Long): Result<TripWithRouteAndBus> =
        withContext(Dispatchers.IO) {
            try {
                val trip = tripDAO.getTripById(tripId)
                if (trip != null) {
                    Result.Success(trip)
                } else {
                    Result.Error(Exception("Not found"), "Trip not found")
                }
            } catch (e: Exception) {
                Result.Error(e, "Error fetching trip: ${e.message}")
            }
        }

    /**
     * ✅ Đếm số ghế còn trống cho một chuyến
     * Dùng: tổng ghế của xe - ghế đã được đặt (CONFIRMED/PENDING)
     */
    override suspend fun getAvailableSeatsCount(tripId: Long, busId: Long): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val total  = seatDAO.getSeatCountForBus(busId)
                val booked = seatDAO.getBookedSeatCountForTrip(tripId)
                Result.Success((total - booked).toInt().coerceAtLeast(0))
            } catch (e: Exception) {
                Result.Error(e, "Lỗi đếm ghế: ${e.message}")
            }
        }

    override suspend fun getSeatAvailability(tripId: Long, busId: Long): Result<TripSeatAvailability> =
        withContext(Dispatchers.IO) {
            try {
                val total = seatDAO.getSeatCountForBus(busId).toInt()
                val booked = seatDAO.getBookedSeatCountForTrip(tripId).toInt()
                Result.Success(TripSeatAvailability(total, booked))
            } catch (e: Exception) {
                Result.Error(e, "Loi dem ghe: ${e.message}")
            }
        }

    /**
     * Create new trip
     */
    override suspend fun createTrip(
        routeId: Long,
        busId: Long,
        departureTime: Long,
        arrivalTime: Long,
        price: Double,
        tripDate: Long
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val trip = Trip(
                routeId = routeId,
                busId = busId,
                departureTime = departureTime,
                arrivalTime = arrivalTime,
                price = price,
                tripDate = tripDate,
                status = "SCHEDULED"
            )
            val id = tripDAO.insertTrip(trip)
            if (id > 0) {
                Result.Success(id)
            } else {
                Result.Error(Exception("Insert failed"), "Could not create trip")
            }
        } catch (e: Exception) {
            Result.Error(e, "Error creating trip: ${e.message}")
        }
    }

    /**
     * Cancel trip
     */
    override suspend fun cancelTrip(tripId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            tripDAO.cancelTrip(tripId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Error cancelling trip: ${e.message}")
        }
    }

    /**
     * Get all trips (for admin)
     */
    override suspend fun getAllTrips(): Result<List<TripWithRouteAndBus>> = withContext(Dispatchers.IO) {
        try {
            val allTrips = tripDAO.getAllTrips().first()
            if (allTrips.isNotEmpty()) {
                Result.Success(allTrips)
            } else {
                Result.Error(Exception("Empty"), "No trips found")
            }
        } catch (e: Exception) {
            Result.Error(e, "Error fetching all trips: ${e.message}")
        }
    }
}
