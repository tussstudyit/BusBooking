package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.Bus
import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.data.entity.Ticket
import com.example.busbooking.data.entity.Trip
import com.example.busbooking.data.entity.User
import com.example.busbooking.data.relations.TicketDetails
import com.example.busbooking.data.relations.TripWithRouteAndBus
import com.example.busbooking.domain.models.Result
import com.example.busbooking.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseTicketRepository(
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) {
    private val auth: FirebaseAuth by lazy(LazyThreadSafetyMode.NONE) { authProvider() }
    private val firestore: FirebaseFirestore by lazy(LazyThreadSafetyMode.NONE) { firestoreProvider() }
    private val tickets get() = firestore.collection("tickets")
    private val users get() = firestore.collection("users")
    private val trips get() = firestore.collection("trips")
    private val routes get() = firestore.collection("routes")
    private val buses get() = firestore.collection("buses")

    suspend fun getUserActiveTickets(userId: Long): Result<List<TicketDetails>> = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid
                ?: return@withContext Result.Error(Exception("No Firebase user"), "Vui lòng đăng nhập lại")

            val details = tickets
                .whereEqualTo("userId", uid)
                .get()
                .await()
                .documents
                .filter { document ->
                    document.getString("status") in ACTIVE_STATUSES
                }
                .mapNotNull { document -> toTicketDetails(document, userId) }
                .sortedByDescending { it.ticket.bookingTime }

            Result.Success(details)
        } catch (e: Exception) {
            Result.Error(e, "Không thể tải vé từ Firestore: ${e.message}")
        }
    }

    suspend fun getTicketById(ticketId: Long): Result<TicketDetails> = withContext(Dispatchers.IO) {
        try {
            val document = findTicketDocument(ticketId)
                ?: return@withContext Result.Error(Exception("Not found"), "Không tìm thấy vé")
            val details = toTicketDetails(document, SessionManager.getCurrentUserId())
                ?: return@withContext Result.Error(Exception("Invalid ticket"), "Dữ liệu vé không hợp lệ")
            Result.Success(details)
        } catch (e: Exception) {
            Result.Error(e, "Không thể tải chi tiết vé từ Firestore: ${e.message}")
        }
    }

    private suspend fun toTicketDetails(document: DocumentSnapshot, fallbackUserId: Long): TicketDetails? {
        val ticket = document.toTicket(fallbackUserId)
        val trip = findTripById(ticket.tripId) ?: return null
        val route = findRouteById(trip.routeId) ?: return null
        val bus = findBusById(trip.busId) ?: return null
        val seat = findSeatById(bus.id, ticket.seatId) ?: Seat(
            id = ticket.seatId,
            busId = bus.id,
            seatNumber = ticket.seatId.toString(),
            floor = 1,
            rowIndex = 0,
            columnIndex = 0,
            isWindow = false,
            isAisle = false,
            seatType = "STANDARD",
            createdAt = 0L
        )
        val user = findUser(document.getString("userId"), ticket.userId)
        return TicketDetails(
            ticket = ticket,
            user = user,
            tripWithRouteAndBus = TripWithRouteAndBus(trip = trip, route = route, bus = bus),
            seat = seat
        )
    }

    private suspend fun findTicketDocument(ticketId: Long): DocumentSnapshot? {
        val direct = tickets.document(ticketId.toString()).get().await()
        if (direct.exists()) return direct

        return tickets
            .whereEqualTo("id", ticketId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
    }

    private suspend fun findUser(uid: String?, fallbackUserId: Long): User {
        val sessionUser = SessionManager.getCurrentUser()
        val document = uid
            ?.takeIf { it.isNotBlank() }
            ?.let { users.document(it).get().await().takeIf { snapshot -> snapshot.exists() } }

        return User(
            id = fallbackUserId,
            name = document?.getString("name") ?: sessionUser?.name.orEmpty(),
            email = document?.getString("email") ?: sessionUser?.email.orEmpty(),
            password = "",
            phone = document?.getString("phone") ?: sessionUser?.phone.orEmpty(),
            role = document?.getString("role") ?: sessionUser?.role.orEmpty(),
            isBlocked = document?.getBoolean("isBlocked") ?: false,
            createdAt = longValue(document?.get("createdAt")) ?: sessionUser?.createdAt ?: 0L
        )
    }

    private suspend fun findTripById(tripId: Long): Trip? {
        val direct = trips.document(tripId.toString()).get().await()
        if (direct.exists()) return direct.toTrip()

        return trips
            .whereEqualTo("id", tripId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toTrip()
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
            .whereEqualTo("id", busId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.id
    }

    private suspend fun findSeatById(busId: Long, seatId: Long): Seat? {
        val busDocumentId = findBusDocumentId(busId) ?: return null
        val seats = buses.document(busDocumentId).collection("seats")
        val direct = seats.document(seatId.toString()).get().await()
        if (direct.exists()) return direct.toSeat(busId)

        return seats
            .whereEqualTo("id", seatId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toSeat(busId)
    }

    private fun DocumentSnapshot.toTicket(fallbackUserId: Long): Ticket {
        return Ticket(
            id = longValue(get("id")) ?: id.toStableLongId(),
            userId = longValue(get("userNumericId")) ?: fallbackUserId,
            tripId = longValue(get("tripId")) ?: 0L,
            seatId = longValue(get("seatId")) ?: 0L,
            bookingTime = longValue(get("bookingTime")) ?: longValue(get("createdAt")) ?: 0L,
            status = getString("status") ?: "PENDING_PAYMENT",
            cancellationReason = getString("cancellationReason"),
            refundAmount = doubleValue(get("refundAmount")),
            refundStatus = getString("refundStatus") ?: "NONE"
        )
    }

    private fun DocumentSnapshot.toRoute(): Route {
        return Route(
            id = longValue(get("id")) ?: id.toStableLongId(),
            origin = getString("origin").orEmpty(),
            destination = getString("destination").orEmpty(),
            distance = (longValue(get("distance")) ?: 0L).toInt(),
            isActive = getBoolean("isActive") ?: true,
            createdAt = longValue(get("createdAt")) ?: 0L
        )
    }

    private fun DocumentSnapshot.toBus(): Bus {
        return Bus(
            id = longValue(get("id")) ?: id.toStableLongId(),
            busName = getString("busName").orEmpty(),
            totalSeats = (longValue(get("totalSeats")) ?: 0L).toInt(),
            licensePlate = getString("licensePlate").orEmpty(),
            seatLayoutJson = getString("seatLayoutJson").orEmpty(),
            isActive = getBoolean("isActive") ?: true,
            createdAt = longValue(get("createdAt")) ?: 0L
        )
    }

    private fun DocumentSnapshot.toTrip(): Trip {
        return Trip(
            id = longValue(get("id")) ?: id.toStableLongId(),
            routeId = longValue(get("routeId")) ?: 0L,
            busId = longValue(get("busId")) ?: 0L,
            departureTime = longValue(get("departureTime")) ?: 0L,
            arrivalTime = longValue(get("arrivalTime")) ?: 0L,
            price = doubleValue(get("price")) ?: 0.0,
            tripDate = longValue(get("tripDate")) ?: 0L,
            status = getString("status") ?: "SCHEDULED",
            createdAt = longValue(get("createdAt")) ?: 0L
        )
    }

    private fun DocumentSnapshot.toSeat(busId: Long): Seat {
        return Seat(
            id = longValue(get("id")) ?: id.toStableLongId(),
            busId = longValue(get("busId")) ?: busId,
            seatNumber = getString("seatNumber").orEmpty(),
            floor = (longValue(get("floor")) ?: 1L).toInt(),
            rowIndex = (longValue(get("rowIndex")) ?: 0L).toInt(),
            columnIndex = (longValue(get("columnIndex")) ?: 0L).toInt(),
            isWindow = getBoolean("isWindow") ?: false,
            isAisle = getBoolean("isAisle") ?: false,
            seatType = getString("seatType") ?: "STANDARD",
            createdAt = longValue(get("createdAt")) ?: 0L
        )
    }

    private fun longValue(value: Any?): Long? = (value as? Number)?.toLong()

    private fun doubleValue(value: Any?): Double? = (value as? Number)?.toDouble()

    private fun String.toStableLongId(): Long {
        return fold(1125899906842597L) { hash, char -> 31 * hash + char.code }
            .let { if (it == Long.MIN_VALUE) 0L else kotlin.math.abs(it) }
    }

    private companion object {
        private val ACTIVE_STATUSES = setOf("PENDING", "PENDING_PAYMENT", "CONFIRMED")
    }
}
