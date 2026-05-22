package com.example.busbooking.domain.repository

import com.example.busbooking.data.dao.RouteDAO
import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.relations.RouteWithTrips
import com.example.busbooking.domain.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface IRouteRepository {
    suspend fun getAllOrigins(): Result<List<String>>
    suspend fun getAllDestinations(): Result<List<String>>
    suspend fun searchRoutes(searchQuery: String): Result<List<Route>>
    suspend fun getRouteByOriginDestination(origin: String, destination: String): Result<Route>
    suspend fun createRoute(origin: String, destination: String, distance: Int): Result<Long>
    suspend fun getRouteWithTrips(routeId: Long): Result<RouteWithTrips>
    suspend fun deactivateRoute(routeId: Long): Result<Unit>
}

class RouteRepository(private val routeDAO: RouteDAO) : IRouteRepository {

    /**
     * Get all origin locations (for dropdown)
     */
    override suspend fun getAllOrigins(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Result.Success(routeDAO.getAllOrigins())
        } catch (e: Exception) {
            Result.Error(e, "Error fetching origins: ${e.message}")
        }
    }

    /**
     * Get all destination locations (for dropdown)
     */
    override suspend fun getAllDestinations(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Result.Success(routeDAO.getAllDestinations())
        } catch (e: Exception) {
            Result.Error(e, "Error fetching destinations: ${e.message}")
        }
    }

    /**
     * Search routes by origin/destination with partial match
     */
    override suspend fun searchRoutes(searchQuery: String): Result<List<Route>> =
        withContext(Dispatchers.IO) {
            try {
                if (searchQuery.isBlank()) {
                    return@withContext Result.Error(
                        Exception("Invalid input"),
                        "Search query cannot be empty"
                    )
                }

                val routes = routeDAO.searchRoutes(searchQuery)

                if (routes.isNotEmpty()) {
                    Result.Success(routes)
                } else {
                    Result.Error(Exception("Not found"), "No routes found")
                }

            } catch (e: Exception) {
                Result.Error(e, "Error searching routes: ${e.message}")
            }
        }

    /**
     * Get route by exact origin and destination
     */
    override suspend fun getRouteByOriginDestination(
        origin: String,
        destination: String
    ): Result<Route> = withContext(Dispatchers.IO) {
        try {
            if (origin.isBlank() || destination.isBlank()) {
                return@withContext Result.Error(
                    Exception("Invalid input"),
                    "Origin and destination cannot be empty"
                )
            }

            val route = routeDAO.getRouteByOriginDestination(origin, destination)

            if (route != null) {
                Result.Success(route)
            } else {
                Result.Error(Exception("Not found"), "No route found")
            }

        } catch (e: Exception) {
            Result.Error(e, "Error fetching route: ${e.message}")
        }
    }

    /**
     * Create new route
     */
    override suspend fun createRoute(
        origin: String,
        destination: String,
        distance: Int
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (origin.isBlank() || destination.isBlank()) {
                return@withContext Result.Error(
                    Exception("Invalid input"),
                    "Origin/destination cannot be empty"
                )
            }

            val route = Route(
                origin = origin,
                destination = destination,
                distance = distance,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )

            val id = routeDAO.insertRoute(route)

            if (id > 0) Result.Success(id)
            else Result.Error(Exception("Insert failed"), "Could not create route")

        } catch (e: Exception) {
            Result.Error(e, "Error creating route: ${e.message}")
        }
    }

    /**
     * Get route with all its trips
     */
    override suspend fun getRouteWithTrips(routeId: Long): Result<RouteWithTrips> =
        withContext(Dispatchers.IO) {
            try {
                val routeWithTrips = routeDAO.getRouteWithTrips(routeId)

                if (routeWithTrips != null) {
                    Result.Success(routeWithTrips)
                } else {
                    Result.Error(Exception("Not found"), "Route not found")
                }

            } catch (e: Exception) {
                Result.Error(e, "Error fetching route with trips: ${e.message}")
            }
        }

    /**
     * Deactivate route
     */
    override suspend fun deactivateRoute(routeId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                routeDAO.deactivateRoute(routeId)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, "Error deactivating route: ${e.message}")
            }
        }
}
