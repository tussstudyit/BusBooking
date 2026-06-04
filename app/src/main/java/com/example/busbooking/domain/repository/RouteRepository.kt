package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.relations.RouteWithTrips
import com.example.busbooking.domain.models.Result
import java.net.URLEncoder

interface IRouteRepository {
    suspend fun getAllOrigins(): Result<List<String>>
    suspend fun getAllDestinations(): Result<List<String>>
    suspend fun searchRoutes(searchQuery: String): Result<List<Route>>
    suspend fun getRouteByOriginDestination(origin: String, destination: String): Result<Route>
    suspend fun createRoute(origin: String, destination: String, distance: Int): Result<Long>
    suspend fun getRouteWithTrips(routeId: Long): Result<RouteWithTrips>
    suspend fun deactivateRoute(routeId: Long): Result<Unit>
}

class RouteRepository : IRouteRepository {
    override suspend fun getAllOrigins(): Result<List<String>> = api { ApiClient.get("/api/mobile/routes/origins") }
    override suspend fun getAllDestinations(): Result<List<String>> = api { ApiClient.get("/api/mobile/routes/destinations") }

    override suspend fun searchRoutes(searchQuery: String): Result<List<Route>> = api {
        val routes: List<RouteApi> = ApiClient.get("/api/mobile/routes/search?q=${enc(searchQuery)}")
        routes.map { it.toEntity() }
    }

    override suspend fun getRouteByOriginDestination(origin: String, destination: String): Result<Route> = try {
        val routes: List<RouteApi> = ApiClient.get("/api/mobile/routes/search?q=${enc(origin)}")
        routes.firstOrNull { it.origin == origin && it.destination == destination }?.let { Result.Success(it.toEntity()) }
            ?: Result.Error(Exception("Not found"), "KhÃ´ng tÃ¬m tháº¥y tuyáº¿n")
    } catch (e: Exception) {
        Result.Error(e, e.message ?: "KhÃ´ng thá»ƒ táº£i tuyáº¿n")
    }

    override suspend fun createRoute(origin: String, destination: String, distance: Int): Result<Long> = Result.Error(UnsupportedOperationException(), "Táº¡o tuyáº¿n thá»±c hiá»‡n trÃªn web admin")
    override suspend fun getRouteWithTrips(routeId: Long): Result<RouteWithTrips> = Result.Error(UnsupportedOperationException(), "KhÃ´ng há»— trá»£")
    override suspend fun deactivateRoute(routeId: Long): Result<Unit> = Result.Error(UnsupportedOperationException(), "Quáº£n trá»‹ tuyáº¿n thá»±c hiá»‡n trÃªn web admin")

    private suspend fun <T> api(block: suspend () -> T): Result<T> = try { Result.Success(block()) } catch (e: Exception) { Result.Error(e, e.message ?: "Lá»—i káº¿t ná»‘i API") }
    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}

