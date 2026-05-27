package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.Bus
import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.entity.Trip
import com.example.busbooking.data.relations.TripWithRouteAndBus
import com.example.busbooking.domain.models.Result
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class FirebaseTripRepository(
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : ITripRepository {

    private val firestore: FirebaseFirestore by lazy(LazyThreadSafetyMode.NONE) { firestoreProvider() }
    private val routes get() = firestore.collection("routes")
    private val buses get() = firestore.collection("buses")
    private val trips get() = firestore.collection("trips")
    private val tripSeats get() = firestore.collection("tripSeats")

    override suspend fun searchTrips(
        origin: String,
        destination: String,
        tripDate: Long
    ): Result<List<TripWithRouteAndBus>> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val route = findRouteByOriginDestination(origin, destination)
                ?: return@withContext Result.Success(emptyList())

            val dayStart = Calendar.getInstance().apply {
                timeInMillis = tripDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val dayEnd = dayStart + 86_400_000L
            val tripDocuments = trips
                .whereEqualTo("routeId", route.id)
                .get()
                .await()
                .documents
                .filter {
                    it.getString("status") == "SCHEDULED" &&
                        (it.getLong("tripDate") ?: 0L) >= dayStart &&
                        (it.getLong("tripDate") ?: 0L) < dayEnd
                }

            val hydrated = tripDocuments
                .filter { (it.getLong("departureTime") ?: 0L) > now }
                .mapNotNull { document ->
                    val trip = document.toTrip()
                    val bus = findBusById(trip.busId) ?: return@mapNotNull null
                    TripWithRouteAndBus(trip = trip, route = route, bus = bus)
                }
                .sortedBy { it.trip.departureTime }

            Result.Success(hydrated)
        } catch (e: Exception) {
            Result.Error(e, "Kh\u00f4ng th\u1ec3 t\u1ea3i chuy\u1ebfn xe: ${e.message}")
        }
    }

    override suspend fun getUpcomingTripsForRoute(
        routeId: Long,
        fromDate: Long
    ): Result<List<TripWithRouteAndBus>> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val route = findRouteById(routeId)
                ?: return@withContext Result.Error(Exception("Not found"), "Route not found")

            val hydrated = trips
                .whereEqualTo("routeId", routeId)
                .get()
                .await()
                .documents
                .filter {
                    it.getString("status") == "SCHEDULED" &&
                        (it.getLong("tripDate") ?: 0L) >= fromDate &&
                        (it.getLong("departureTime") ?: 0L) > now
                }
                .mapNotNull { document ->
                    val trip = document.toTrip()
                    val bus = findBusById(trip.busId) ?: return@mapNotNull null
                    TripWithRouteAndBus(trip = trip, route = route, bus = bus)
                }
                .sortedWith(compareBy<TripWithRouteAndBus> { it.trip.tripDate }.thenBy { it.trip.departureTime })

            Result.Success(hydrated)
        } catch (e: Exception) {
            Result.Error(e, "Error fetching upcoming trips: ${e.message}")
        }
    }

    override suspend fun getTripById(tripId: Long): Result<TripWithRouteAndBus> =
        withContext(Dispatchers.IO) {
            try {
                val trip = findTripById(tripId)
                    ?: return@withContext Result.Error(Exception("Not found"), "Kh\u00f4ng t\u00ecm th\u1ea5y chuy\u1ebfn xe")
                val route = findRouteById(trip.routeId)
                    ?: return@withContext Result.Error(Exception("Not found"), "Route not found")
                val bus = findBusById(trip.busId)
                    ?: return@withContext Result.Error(Exception("Not found"), "Bus not found")

                Result.Success(TripWithRouteAndBus(trip = trip, route = route, bus = bus))
            } catch (e: Exception) {
                Result.Error(e, "Kh\u00f4ng th\u1ec3 t\u1ea3i chi ti\u1ebft chuy\u1ebfn xe: ${e.message}")
            }
        }

    override suspend fun getAvailableSeatsCount(tripId: Long, busId: Long): Result<Int> =
        when (val result = getSeatAvailability(tripId, busId)) {
            is Result.Success -> Result.Success(result.data.availableSeats)
            is Result.Error -> Result.Error(result.exception, result.message)
            else -> Result.Error(Exception("Unknown"), "Không thể đếm ghế trống")
        }

    override suspend fun getSeatAvailability(tripId: Long, busId: Long): Result<TripSeatAvailability> =
        withContext(Dispatchers.IO) {
            try {
                val busDocumentId = findBusDocumentId(busId)
                    ?: return@withContext Result.Error(Exception("Not found"), "Không tìm thấy xe")

                val busSnapshot = buses.document(busDocumentId).get().await()
                val configuredTotal = (busSnapshot.getLong("totalSeats") ?: 0L).toInt()
                val generatedTotal = buses.document(busDocumentId)
                    .collection("seats")
                    .get()
                    .await()
                    .size()
                val total = if (generatedTotal > 0) generatedTotal else configuredTotal

                val booked = tripSeats
                    .whereEqualTo("tripId", tripId)
                    .whereIn("status", listOf("CONFIRMED", "USED"))
                    .get()
                    .await()
                    .size()

                Result.Success(TripSeatAvailability(total, booked))
            } catch (e: Exception) {
                Result.Error(e, "Lỗi đếm ghế Firebase: ${e.message}")
            }
        }

    override suspend fun createTrip(
        routeId: Long,
        busId: Long,
        departureTime: Long,
        arrivalTime: Long,
        price: Double,
        tripDate: Long
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val doc = trips.document()
            val tripId = doc.id.toStableLongId()
            val data = mapOf(
                "id" to tripId,
                "routeId" to routeId,
                "busId" to busId,
                "departureTime" to departureTime,
                "arrivalTime" to arrivalTime,
                "price" to price,
                "tripDate" to tripDate,
                "status" to "SCHEDULED",
                "createdAt" to System.currentTimeMillis()
            )
            doc.set(data).await()
            Result.Success(tripId)
        } catch (e: Exception) {
            Result.Error(e, "Error creating trip: ${e.message}")
        }
    }

    override suspend fun cancelTrip(tripId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val document = findTripDocument(tripId)
                ?: return@withContext Result.Error(Exception("Not found"), "Trip not found")

            document.reference.update("status", "CANCELLED").await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Error cancelling trip: ${e.message}")
        }
    }

    override suspend fun getAllTrips(): Result<List<TripWithRouteAndBus>> =
        withContext(Dispatchers.IO) {
            try {
                val allTrips = trips
                    .whereEqualTo("status", "SCHEDULED")
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document ->
                        val trip = document.toTrip()
                        val route = findRouteById(trip.routeId) ?: return@mapNotNull null
                        val bus = findBusById(trip.busId) ?: return@mapNotNull null
                        TripWithRouteAndBus(trip = trip, route = route, bus = bus)
                    }
                    .sortedWith(compareByDescending<TripWithRouteAndBus> { it.trip.tripDate }.thenBy { it.trip.departureTime })

                Result.Success(allTrips)
            } catch (e: Exception) {
                Result.Error(e, "Error fetching all trips: ${e.message}")
            }
        }

    private suspend fun findRouteByOriginDestination(origin: String, destination: String): Route? {
        return routes
            .whereEqualTo("isActive", true)
            .get()
            .await()
            .documents
            .firstOrNull {
                it.getString("origin") == origin &&
                    it.getString("destination") == destination
            }
            ?.toRoute()
    }

    private suspend fun findRouteById(routeId: Long): Route? {
        val direct = routes.document(routeId.toString()).get().await()
        if (direct.exists()) return direct.toRoute()

        return routes
            .whereEqualTo("id", routeId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.takeIf { it.getBoolean("isActive") ?: true }
            ?.toRoute()
    }

    private suspend fun findBusById(busId: Long): Bus? {
        val documentId = findBusDocumentId(busId) ?: return null
        return buses.document(documentId).get().await().toBus()
    }

    private suspend fun findBusDocumentId(busId: Long): String? {
        val direct = buses.document(busId.toString()).get().await()
        if (direct.exists()) return direct.id

        return buses
            .whereEqualTo("isActive", true)
            .whereEqualTo("id", busId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.id
    }

    private suspend fun findTripById(tripId: Long): Trip? {
        return findTripDocument(tripId)?.toTrip()
    }

    private suspend fun findTripDocument(tripId: Long): DocumentSnapshot? {
        val direct = trips.document(tripId.toString()).get().await()
        if (direct.exists()) return direct

        return trips
            .whereEqualTo("id", tripId)
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

    private fun DocumentSnapshot.toBus(): Bus {
        return Bus(
            id = getLong("id") ?: id.toStableLongId(),
            busName = getString("busName").orEmpty(),
            totalSeats = (getLong("totalSeats") ?: 0L).toInt(),
            licensePlate = getString("licensePlate").orEmpty(),
            seatLayoutJson = getString("seatLayoutJson").orEmpty(),
            isActive = getBoolean("isActive") ?: true,
            createdAt = getLong("createdAt") ?: 0L
        )
    }

    private fun DocumentSnapshot.toTrip(): Trip {
        return Trip(
            id = getLong("id") ?: id.toStableLongId(),
            routeId = getLong("routeId") ?: 0L,
            busId = getLong("busId") ?: 0L,
            departureTime = getLong("departureTime") ?: 0L,
            arrivalTime = getLong("arrivalTime") ?: 0L,
            price = getDouble("price") ?: 0.0,
            tripDate = getLong("tripDate") ?: 0L,
            status = getString("status") ?: "SCHEDULED",
            createdAt = getLong("createdAt") ?: 0L
        )
    }

    private fun String.toStableLongId(): Long {
        return fold(1125899906842597L) { hash, char -> 31 * hash + char.code }
            .let { if (it == Long.MIN_VALUE) 0L else kotlin.math.abs(it) }
    }

}
