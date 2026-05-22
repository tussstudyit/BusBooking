package com.example.busbooking.domain.repository

import android.util.Log
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.domain.models.SeatDisplay
import com.example.busbooking.domain.models.SeatReservationResult
import com.example.busbooking.domain.models.SeatStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseSeatRepository(
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) {
    private val auth: FirebaseAuth by lazy(LazyThreadSafetyMode.NONE) { authProvider() }
    private val firestore: FirebaseFirestore by lazy(LazyThreadSafetyMode.NONE) { firestoreProvider() }
    private val buses get() = firestore.collection("buses")
    private val trips get() = firestore.collection("trips")
    private val tickets get() = firestore.collection("tickets")
    private val payments get() = firestore.collection("payments")
    private val tripSeats get() = firestore.collection("tripSeats")

    suspend fun getSeatsForTrip(tripId: Long): List<SeatDisplay> = withContext(Dispatchers.IO) {
        val statusBySeatId = runCatching { seatStatusesForTrip(tripId) }
            .onFailure { Log.w(TAG, "Cannot load trip seat statuses; tripId=$tripId", it) }
            .getOrDefault(emptyMap())

        try {
            val trip = runCatching { findTrip(tripId) }
                .onFailure { Log.w(TAG, "Cannot load trip document; using default layout, tripId=$tripId", it) }
                .getOrNull()

            val busId = trip?.let { numberAsLong(it.get("busId")) } ?: 0L
            if (trip == null) {
                Log.w(TAG, "Trip document not found; using default layout, tripId=$tripId")
                return@withContext defaultSeatDisplays(busId, statusBySeatId)
            }
            if (busId <= 0L) {
                Log.w(TAG, "Trip ${trip.id} has no busId; using default layout, tripId=$tripId")
                return@withContext defaultSeatDisplays(busId, statusBySeatId)
            }

            val busDocumentId = runCatching { findBusDocumentId(busId) }
                .onFailure { Log.w(TAG, "Cannot find bus document; using default layout, busId=$busId", it) }
                .getOrNull()

            val seats = if (busDocumentId == null) {
                Log.w(TAG, "Bus document not found; using default layout, busId=$busId")
                emptyList()
            } else {
                runCatching { loadBusSeats(busDocumentId, busId) }
                    .onFailure { Log.w(TAG, "Cannot load bus seats; using default layout, busId=$busId", it) }
                    .getOrDefault(emptyList())
            }

            val layoutSeats = seats.ifEmpty {
                Log.w(TAG, "Bus $busDocumentId has no seat layout; using default layout for tripId=$tripId")
                defaultSeats(busId)
            }

            layoutSeats
                .sortedWith(compareBy<Seat> { it.floor }.thenBy { it.id })
                .map { seat -> SeatDisplay(seat, statusBySeatId[seat.id] ?: SeatStatus.AVAILABLE) }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot load seats for trip; using default layout, tripId=$tripId", e)
            defaultSeatDisplays(0L, statusBySeatId)
        }
    }

    suspend fun reserveSeats(
        userId: Long,
        tripId: Long,
        selectedSeats: List<Seat>,
        amountPerSeat: Double
    ): SeatReservationResult = withContext(Dispatchers.IO) {
        if (selectedSeats.isEmpty()) {
            return@withContext SeatReservationResult.Failure("Vui lòng chọn ghế trước khi tiếp tục")
        }

        val userUid = runCatching { auth.currentUser?.uid }.getOrNull() ?: "demo-user"

        try {
            val tripDocumentId = findTripDocumentId(tripId)
                ?: return@withContext SeatReservationResult.Failure("Chuyến xe không hợp lệ")

            firestore.runTransaction { transaction ->
                val tripSnapshot = transaction.get(trips.document(tripDocumentId))
                if (!tripSnapshot.exists()) {
                    throw IllegalStateException("Chuyến xe không hợp lệ")
                }
                if (tripSnapshot.getString("status") != "SCHEDULED") {
                    throw IllegalStateException("Chuyến xe không còn mở đặt vé")
                }

                val busId = numberAsLong(tripSnapshot.get("busId")) ?: 0L
                val now = System.currentTimeMillis()
                val holdExpiresAt = now + HOLD_DURATION_MS
                val paymentRef = payments.document()
                val paymentId = paymentRef.id
                val ticketIds = mutableListOf<Long>()
                val ticketDocumentIds = mutableListOf<String>()
                val tripSeatIds = mutableListOf<String>()

                val tripSeatRefs = selectedSeats.map { seat ->
                    seat to tripSeats.document("${tripId}_${seat.id}")
                }
                val tripSeatSnapshots = tripSeatRefs.map { (seat, ref) ->
                    Triple(seat, ref, transaction.get(ref))
                }

                tripSeatSnapshots.forEach { (seat, _, snapshot) ->
                    if (isSeatTaken(snapshot.getString("status"), numberAsLong(snapshot.get("holdExpiresAt")), now)) {
                        throw SeatTakenException(seat.seatNumber)
                    }
                }

                tripSeatSnapshots.forEachIndexed { index, (seat, tripSeatRef, _) ->
                    val ticketRef = tickets.document()
                    val ticketId = ticketRef.id.toStableLongId()
                    ticketIds += ticketId
                    ticketDocumentIds += ticketRef.id
                    tripSeatIds += tripSeatRef.id

                    transaction.set(ticketRef, mapOf(
                        "id" to ticketId,
                        "userId" to userUid,
                        "userNumericId" to userId,
                        "tripId" to tripId,
                        "seatId" to seat.id,
                        "busId" to busId,
                        "paymentId" to paymentId,
                        "bookingTime" to now + index,
                        "status" to "PENDING_PAYMENT",
                        "cancellationReason" to "",
                        "refundAmount" to 0.0,
                        "refundStatus" to "NONE",
                        "createdAt" to now
                    ))

                    transaction.set(tripSeatRef, mapOf(
                        "tripId" to tripId,
                        "seatId" to seat.id,
                        "seatNumber" to seat.seatNumber,
                        "ticketId" to ticketId,
                        "ticketDocumentId" to ticketRef.id,
                        "paymentId" to paymentId,
                        "userId" to userUid,
                        "userNumericId" to userId,
                        "status" to "PENDING_PAYMENT",
                        "holdExpiresAt" to holdExpiresAt,
                        "createdAt" to now,
                        "updatedAt" to now
                    ))
                }

                transaction.set(paymentRef, mapOf(
                    "id" to paymentRef.id.toStableLongId(),
                    "ticketId" to ticketDocumentIds.firstOrNull().orEmpty(),
                    "ticketIds" to ticketIds,
                    "ticketDocumentIds" to ticketDocumentIds,
                    "tripSeatIds" to tripSeatIds,
                    "userId" to userUid,
                    "userNumericId" to userId,
                    "tripId" to tripId,
                    "seatId" to (selectedSeats.firstOrNull()?.id ?: 0L),
                    "seatIds" to selectedSeats.map { it.id },
                    "amount" to amountPerSeat * selectedSeats.size,
                    "provider" to "VNPAY",
                    "status" to "CREATED",
                    "holdExpiresAt" to holdExpiresAt,
                    "createdAt" to now,
                    "updatedAt" to now
                ))

                SeatReservationResult.Success(ticketIds, paymentId)
            }.await()
        } catch (e: SeatTakenException) {
            SeatReservationResult.AlreadyTaken(e.seatNumber)
        } catch (e: Exception) {
            SeatReservationResult.Failure(e.message ?: "Không thể tạo yêu cầu thanh toán trên Firebase")
        }
    }

    private suspend fun loadBusSeats(busDocumentId: String, busId: Long): List<Seat> {
        return buses.document(busDocumentId)
            .collection("seats")
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                val seatId = numberAsLong(document.get("id"))
                    ?: document.id.toLongOrNull()
                    ?: return@mapNotNull null
                val index = seatId.toInt()
                val seatNumber = document.getString("seatNumber")
                    ?.takeIf { it.isNotBlank() }
                    ?: seatNumber(index)
                Seat(
                    id = seatId,
                    busId = numberAsLong(document.get("busId")) ?: busId,
                    seatNumber = seatNumber,
                    floor = numberAsInt(document.get("floor")) ?: if (index <= 17) 1 else 2,
                    rowIndex = numberAsInt(document.get("rowIndex")) ?: ((index - 1) % 17) / 3,
                    columnIndex = numberAsInt(document.get("columnIndex")) ?: (index - 1) % 3,
                    isWindow = document.getBoolean("isWindow") ?: false,
                    isAisle = document.getBoolean("isAisle") ?: false,
                    seatType = document.getString("seatType") ?: "STANDARD",
                    createdAt = numberAsLong(document.get("createdAt")) ?: 0L
                )
            }
    }

    private suspend fun findTrip(tripId: Long) = trips.document(tripId.toString()).get().await().takeIf { it.exists() }
        ?: trips
            .whereEqualTo("id", tripId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()

    private suspend fun findTripDocumentId(tripId: Long): String? = findTrip(tripId)?.id

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

    private suspend fun seatStatusesForTrip(tripId: Long): Map<Long, SeatStatus> {
        return tripSeats
            .whereEqualTo("tripId", tripId)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                val seatId = numberAsLong(document.get("seatId")) ?: return@mapNotNull null
                seatId to statusFromTripSeat(document.getString("status"))
            }
            .toMap()
    }

    private fun statusFromTripSeat(status: String?): SeatStatus {
        return when (status) {
            "CONFIRMED", "USED" -> SeatStatus.BOOKED
            else -> SeatStatus.AVAILABLE
        }
    }

    private fun isSeatTaken(status: String?, holdExpiresAt: Long?, now: Long): Boolean {
        return when (status) {
            "CONFIRMED", "USED" -> true
            "PENDING_PAYMENT" -> holdExpiresAt != null && holdExpiresAt > now
            else -> false
        }
    }

    private fun defaultSeatDisplays(
        busId: Long,
        statusBySeatId: Map<Long, SeatStatus>
    ): List<SeatDisplay> {
        return defaultSeats(busId)
            .map { seat -> SeatDisplay(seat, statusBySeatId[seat.id] ?: SeatStatus.AVAILABLE) }
    }

    private fun defaultSeats(busId: Long): List<Seat> {
        return (1..34).map { index ->
            Seat(
                id = index.toLong(),
                busId = busId,
                seatNumber = seatNumber(index),
                floor = if (index <= 17) 1 else 2,
                rowIndex = ((index - 1) % 17) / 3,
                columnIndex = (index - 1) % 3,
                isWindow = index % 3 == 1 || index % 3 == 0,
                isAisle = index % 3 == 2,
                seatType = "STANDARD",
                createdAt = 0L
            )
        }
    }

    private fun seatNumber(index: Int): String {
        val prefix = if (index <= 17) "A" else "B"
        val number = if (index <= 17) index else index - 17
        return "$prefix$number"
    }

    private fun numberAsLong(value: Any?): Long? = (value as? Number)?.toLong()

    private fun numberAsInt(value: Any?): Int? = (value as? Number)?.toInt()

    private fun String.toStableLongId(): Long {
        return fold(1125899906842597L) { hash, char -> 31 * hash + char.code }
            .let { if (it == Long.MIN_VALUE) 0L else kotlin.math.abs(it) }
    }

    private class SeatTakenException(val seatNumber: String) : RuntimeException()

    private companion object {
        private const val TAG = "FirebaseSeatRepository"
        private const val HOLD_DURATION_MS = 15 * 60_000L
    }
}
