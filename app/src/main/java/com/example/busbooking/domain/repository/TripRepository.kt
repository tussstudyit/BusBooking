package com.example.busbooking.domain.repository

import com.example.busbooking.data.dao.RouteDAO
import com.example.busbooking.data.dao.TripDAO
import com.example.busbooking.data.entity.Trip
import com.example.busbooking.data.relations.TripWithRouteAndBus
import com.example.busbooking.domain.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

class TripRepository(
    private val tripDAO: TripDAO,
    private val routeDAO: RouteDAO
) {

    /**
     * Search trips by origin, destination, and date
     *
     * @param origin Starting location
     * @param destination Ending location
     * @param tripDate Trip date as timestamp (Long)
     */
    suspend fun searchTrips(
        origin: String,
        destination: String,
        tripDate: Long
    ): Result<List<TripWithRouteAndBus>> = withContext(Dispatchers.IO) {
        try {
            // Normalize về 00:00:00 local timezone (tránh lệch UTC)
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
     *
     * @param routeId Route identifier
     * @param fromDate Starting date (timestamp)
     */
    suspend fun getUpcomingTripsForRoute(
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
    suspend fun getTripById(tripId: Long): Result<TripWithRouteAndBus> = 
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
     * Create new trip
     */
    suspend fun createTrip(
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
    suspend fun cancelTrip(tripId: Long): Result<Unit> = withContext(Dispatchers.IO) {
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
    suspend fun getAllTrips(): Result<List<TripWithRouteAndBus>> = withContext(Dispatchers.IO) {
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