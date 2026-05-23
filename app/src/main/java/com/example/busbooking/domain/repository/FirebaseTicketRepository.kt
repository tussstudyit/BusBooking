package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.Bus
import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.data.entity.Ticket
import com.example.busbooking.data.entity.Trip
import com.example.busbooking.data.entity.User
import com.example.busbooking.data.relations.TicketDetails
import com.example.busbooking.data.relations.TripWithRouteAndBus
import com.example.busbooking.data.relations.isTicketHistory
import com.example.busbooking.data.relations.isUpcomingTicket
import com.example.busbooking.data.relations.tripScheduleMillis
import com.example.busbooking.domain.models.Result
import com.example.busbooking.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class PendingPaymentSession(
    val paymentId: String,
    val amount: Double,
    val expiresAt: Long
)

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
    private val payments get() = firestore.collection("payments")

    suspend fun getUserActiveTickets(userId: Long): Result<List<TicketDetails>> = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank() && userId <= 0L) {
                return@withContext Result.Error(
                    Exception("No Firebase user"),
                    "Vui l\u00f2ng \u0111\u0103ng nh\u1eadp l\u1ea1i"
                )
            }

            val documents = findUserTicketDocuments(userId)
            val now = System.currentTimeMillis()

            val details = documents.values
                .mapNotNull { document ->
                    val status = document.getString("status")
                    if (status !in ACTIVE_STATUSES) return@mapNotNull null
                    if (status in PENDING_PAYMENT_STATUSES && isPendingPaymentExpired(document)) {
                        runCatching { expirePendingPayment(document) }
                        return@mapNotNull null
                    }
                    toTicketDetails(document, userId)
                }
                .filter { it.isUpcomingTicket(now) }
                .sortedWith(
                    compareBy<TicketDetails> { it.tripScheduleMillis().takeIf { time -> time > 0L } ?: Long.MAX_VALUE }
                        .thenByDescending { it.ticket.bookingTime }
                )

            Result.Success(details)
        } catch (e: Exception) {
            Result.Error(e, "Kh\u00f4ng th\u1ec3 t\u1ea3i v\u00e9 t\u1eeb Firestore: ${e.message}")
        }
    }

    suspend fun getUserTicketHistory(userId: Long): Result<List<TicketDetails>> = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank() && userId <= 0L) {
                return@withContext Result.Error(
                    Exception("No Firebase user"),
                    "Vui l\u00f2ng \u0111\u0103ng nh\u1eadp l\u1ea1i"
                )
            }

            val documents = findUserTicketDocuments(userId)
            val now = System.currentTimeMillis()
            val details = documents.values
                .mapNotNull { document ->
                    if (document.getString("status") in PENDING_PAYMENT_STATUSES && isPendingPaymentExpired(document)) {
                        runCatching { expirePendingPayment(document) }
                        return@mapNotNull null
                    }
                    toTicketDetails(document, userId)
                }
                .filter { it.isTicketHistory(now) }
                .sortedWith(
                    compareByDescending<TicketDetails> { it.tripScheduleMillis() }
                        .thenByDescending { it.ticket.bookingTime }
                )

            Result.Success(details)
        } catch (e: Exception) {
            Result.Error(e, "Kh\u00f4ng th\u1ec3 t\u1ea3i l\u1ecbch s\u1eed v\u00e9 t\u1eeb Firestore: ${e.message}")
        }
    }

    suspend fun getTicketById(ticketId: Long): Result<TicketDetails> = withContext(Dispatchers.IO) {
        try {
            val document = findTicketDocument(ticketId)
                ?: return@withContext Result.Error(Exception("Not found"), "Kh\u00f4ng t\u00ecm th\u1ea5y v\u00e9")
            if (document.getString("status") in PENDING_PAYMENT_STATUSES && isPendingPaymentExpired(document)) {
                runCatching { expirePendingPayment(document) }
                return@withContext Result.Error(Exception("Expired"), "Phi\u00ean thanh to\u00e1n \u0111\u00e3 qu\u00e1 5 ph\u00fat")
            }
            val details = toTicketDetails(document, SessionManager.getCurrentUserId())
                ?: return@withContext Result.Error(Exception("Invalid ticket"), "D\u1eef li\u1ec7u v\u00e9 kh\u00f4ng h\u1ee3p l\u1ec7")
            Result.Success(details)
        } catch (e: Exception) {
            Result.Error(e, "Kh\u00f4ng th\u1ec3 t\u1ea3i chi ti\u1ebft v\u00e9 t\u1eeb Firestore: ${e.message}")
        }
    }

    suspend fun getPendingPaymentSession(ticketId: Long): Result<PendingPaymentSession> = withContext(Dispatchers.IO) {
        try {
            val ticket = findTicketDocument(ticketId)
                ?: return@withContext Result.Error(Exception("Not found"), "Kh\u00f4ng t\u00ecm th\u1ea5y v\u00e9")
            val status = ticket.getString("status")
            if (status !in PENDING_PAYMENT_STATUSES) {
                return@withContext Result.Error(Exception("Invalid status"), "V\u00e9 n\u00e0y kh\u00f4ng c\u00f2n ch\u1edd thanh to\u00e1n")
            }
            if (isPendingPaymentExpired(ticket)) {
                runCatching { expirePendingPayment(ticket) }
                return@withContext Result.Error(Exception("Expired"), "Phi\u00ean thanh to\u00e1n \u0111\u00e3 qu\u00e1 5 ph\u00fat")
            }

            val paymentId = ticket.getString("paymentId").orEmpty()
            if (paymentId.isBlank()) {
                return@withContext Result.Error(Exception("Missing paymentId"), "V\u00e9 ch\u01b0a c\u00f3 m\u00e3 thanh to\u00e1n")
            }

            val payment = payments.document(paymentId).get().await()
            if (!payment.exists()) {
                return@withContext Result.Error(Exception("Payment not found"), "Kh\u00f4ng t\u00ecm th\u1ea5y phi\u00ean thanh to\u00e1n")
            }
            if (payment.getString("status") in PAYMENT_TERMINAL_STATUSES) {
                return@withContext Result.Error(Exception("Payment closed"), "Phi\u00ean thanh to\u00e1n \u0111\u00e3 k\u1ebft th\u00fac")
            }

            val expiresAt = pendingPaymentExpiresAt(ticket, payment)
            if (System.currentTimeMillis() > expiresAt) {
                runCatching { expirePendingPayment(ticket) }
                return@withContext Result.Error(Exception("Expired"), "Phi\u00ean thanh to\u00e1n \u0111\u00e3 qu\u00e1 5 ph\u00fat")
            }

            Result.Success(
                PendingPaymentSession(
                    paymentId = paymentId,
                    amount = doubleValue(payment.get("amount")) ?: 0.0,
                    expiresAt = expiresAt
                )
            )
        } catch (e: Exception) {
            Result.Error(e, "Kh\u00f4ng th\u1ec3 t\u1ea3i phi\u00ean thanh to\u00e1n: ${e.message}")
        }
    }

    suspend fun cancelPendingPayment(ticketId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ticket = findTicketDocument(ticketId)
                ?: return@withContext Result.Error(Exception("Not found"), "Kh\u00f4ng t\u00ecm th\u1ea5y v\u00e9")
            val status = ticket.getString("status")
            if (status !in PENDING_PAYMENT_STATUSES) {
                return@withContext Result.Error(Exception("Invalid status"), "Ch\u1ec9 c\u00f3 th\u1ec3 h\u1ee7y v\u00e9 \u0111ang ch\u1edd thanh to\u00e1n")
            }

            updatePendingPaymentTickets(
                ticket = ticket,
                ticketStatus = "CANCELLED",
                paymentStatus = "CANCELLED",
                reason = "Ng\u01b0\u1eddi d\u00f9ng h\u1ee7y thanh to\u00e1n"
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Kh\u00f4ng th\u1ec3 h\u1ee7y thanh to\u00e1n: ${e.message}")
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

    private suspend fun findUserTicketDocuments(userId: Long): LinkedHashMap<String, DocumentSnapshot> {
        val uid = auth.currentUser?.uid
        val documents = linkedMapOf<String, DocumentSnapshot>()
        if (!uid.isNullOrBlank()) {
            tickets.whereEqualTo("userId", uid)
                .get()
                .await()
                .documents
                .forEach { documents[it.id] = it }
        }
        if (userId > 0L) {
            tickets.whereEqualTo("userNumericId", userId)
                .get()
                .await()
                .documents
                .forEach { documents[it.id] = it }
        }
        return documents
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

    private fun isPendingPaymentExpired(ticket: DocumentSnapshot): Boolean {
        return System.currentTimeMillis() > pendingPaymentExpiresAt(ticket)
    }

    private fun pendingPaymentExpiresAt(
        ticket: DocumentSnapshot,
        payment: DocumentSnapshot? = null
    ): Long {
        val createdAt = longValue(payment?.get("createdAt"))
            ?: longValue(ticket.get("createdAt"))
            ?: longValue(ticket.get("bookingTime"))
            ?: 0L
        return createdAt + PENDING_PAYMENT_TIMEOUT_MS
    }

    private suspend fun expirePendingPayment(ticket: DocumentSnapshot) {
        updatePendingPaymentTickets(
            ticket = ticket,
            ticketStatus = "PAYMENT_FAILED",
            paymentStatus = "EXPIRED",
            reason = "Phi\u00ean thanh to\u00e1n qu\u00e1 5 ph\u00fat"
        )
    }

    private suspend fun updatePendingPaymentTickets(
        ticket: DocumentSnapshot,
        ticketStatus: String,
        paymentStatus: String,
        reason: String
    ) {
        val paymentId = ticket.getString("paymentId").orEmpty()
        val queriedTickets = if (paymentId.isBlank()) {
            emptyList()
        } else {
            tickets.whereEqualTo("paymentId", paymentId).get().await().documents
        }
        val relatedTickets = queriedTickets.ifEmpty { listOf(ticket) }

        val now = System.currentTimeMillis()
        val batch = firestore.batch()
        relatedTickets
            .filter { it.getString("status") in PENDING_PAYMENT_STATUSES }
            .forEach { document ->
                batch.update(document.reference, mapOf(
                    "status" to ticketStatus,
                    "cancellationReason" to reason,
                    "updatedAt" to now
                ))
            }
        if (paymentId.isNotBlank()) {
            batch.set(payments.document(paymentId), mapOf(
                "status" to paymentStatus,
                "failureReason" to reason,
                "updatedAt" to now
            ), SetOptions.merge())
        }
        batch.commit().await()
    }

    private fun String.toStableLongId(): Long {
        return fold(1125899906842597L) { hash, char -> 31 * hash + char.code }
            .let { if (it == Long.MIN_VALUE) 0L else kotlin.math.abs(it) }
    }

    private companion object {
        private const val PENDING_PAYMENT_TIMEOUT_MS = 5 * 60 * 1000L
        private val ACTIVE_STATUSES = setOf("PENDING", "PENDING_PAYMENT", "CONFIRMED")
        private val PENDING_PAYMENT_STATUSES = setOf("PENDING", "PENDING_PAYMENT")
        private val PAYMENT_TERMINAL_STATUSES = setOf("SUCCESS", "FAILED", "EXPIRED", "CANCELLED")
    }
}
