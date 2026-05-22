package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.relations.RouteWithTrips
import com.example.busbooking.domain.models.Result
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseRouteRepository(
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : IRouteRepository {

    private val firestore: FirebaseFirestore by lazy(LazyThreadSafetyMode.NONE) { firestoreProvider() }
    private val routes get() = firestore.collection("routes")

    override suspend fun getAllOrigins(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val origins = routes
                .whereEqualTo("isActive", true)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("origin") }
                .distinct()
                .sorted()

            Result.Success(origins.ifEmpty { DemoBookingData.locations().first })
        } catch (e: Exception) {
            Result.Success(DemoBookingData.locations().first)
        }
    }

    override suspend fun getAllDestinations(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val destinations = routes
                .whereEqualTo("isActive", true)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("destination") }
                .distinct()
                .sorted()

            Result.Success(destinations.ifEmpty { DemoBookingData.locations().second })
        } catch (e: Exception) {
            Result.Success(DemoBookingData.locations().second)
        }
    }

    override suspend fun searchRoutes(searchQuery: String): Result<List<Route>> =
        withContext(Dispatchers.IO) {
            try {
                val query = searchQuery.trim()
                val allRoutes = routes
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                    .documents
                    .map { it.toRoute() }

                val filtered = if (query.isBlank()) {
                    allRoutes
                } else {
                    allRoutes.filter {
                        it.origin.contains(query, ignoreCase = true) ||
                            it.destination.contains(query, ignoreCase = true)
                    }
                }.sortedWith(compareBy<Route> { it.origin }.thenBy { it.destination })

                Result.Success(filtered)
            } catch (e: Exception) {
                Result.Error(e, "Error searching routes: ${e.message}")
            }
        }

    override suspend fun getRouteByOriginDestination(
        origin: String,
        destination: String
    ): Result<Route> = withContext(Dispatchers.IO) {
        try {
            val route = routes
                .whereEqualTo("isActive", true)
                .get()
                .await()
                .documents
                .firstOrNull {
                    it.getString("origin") == origin &&
                        it.getString("destination") == destination
                }
                ?.toRoute()

            if (route != null) {
                Result.Success(route)
            } else {
                Result.Error(Exception("Not found"), "No route found")
            }
        } catch (e: Exception) {
            Result.Error(e, "Error fetching route: ${e.message}")
        }
    }

    override suspend fun createRoute(
        origin: String,
        destination: String,
        distance: Int
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val doc = routes.document()
            val routeId = doc.id.toStableLongId()
            val route = mapOf(
                "id" to routeId,
                "origin" to origin,
                "destination" to destination,
                "distance" to distance,
                "isActive" to true,
                "createdAt" to System.currentTimeMillis()
            )
            doc.set(route).await()
            Result.Success(routeId)
        } catch (e: Exception) {
            Result.Error(e, "Error creating route: ${e.message}")
        }
    }

    override suspend fun getRouteWithTrips(routeId: Long): Result<RouteWithTrips> {
        return Result.Error(
            UnsupportedOperationException("Route trips are loaded through TripRepository on Firestore"),
            "Route trips are not available here"
        )
    }

    override suspend fun deactivateRoute(routeId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val document = findRouteDocument(routeId)
                ?: return@withContext Result.Error(Exception("Not found"), "Route not found")

            document.reference.update("isActive", false).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Error deactivating route: ${e.message}")
        }
    }

    private suspend fun findRouteDocument(routeId: Long): DocumentSnapshot? {
        val direct = routes.document(routeId.toString()).get().await()
        if (direct.exists()) return direct

        return routes
            .whereEqualTo("id", routeId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
    }

    private fun DocumentSnapshot.toRoute(): Route {
        return Route(
            id = getLong("id") ?: id.toStableLongId(),
            origin = getString("origin").orEmpty(),
            destination = getString("destination").orEmpty(),
            distance = (getLong("distance") ?: 0L).toInt(),
            isActive = getBoolean("isActive") ?: true,
            createdAt = getLong("createdAt") ?: 0L
        )
    }

    private fun String.toStableLongId(): Long {
        return fold(1125899906842597L) { hash, char -> 31 * hash + char.code }
            .let { if (it == Long.MIN_VALUE) 0L else kotlin.math.abs(it) }
    }
}
