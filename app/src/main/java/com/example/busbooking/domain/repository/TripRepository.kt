package com.example.busbooking.domain.repository

import com.example.busbooking.data.dao.RouteDAO
import com.example.busbooking.data.dao.TripDAO
import com.example.busbooking.data.entity.Trip
import com.example.busbooking.data.relations.TripWithRouteAndBus
import com.example.busbooking.domain.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            if (origin.isBlank() || destination.isBlank()) {
                return@withContext Result.Error(
                    Exception("Invalid input"),
                    "Origin/destination cannot be empty"
                )
            }

            val route = routeDAO.getRouteByOriginDestination(origin, destination)
                ?: return@withContext Result.Error(
                    Exception("Route not found"),
                    "No route found for this origin/destination"
                )

            val trips = tripDAO.getTripsForRouteAndDate(route.id, tripDate)

            if (trips.isNotEmpty()) {
                Result.Success(trips)
            } else {
                Result.Error(Exception("Empty"), "No trips available")
            }

        } catch (e: Exception) {
            Result.Error(e, "Error searching trips: ${e.message}")
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
}