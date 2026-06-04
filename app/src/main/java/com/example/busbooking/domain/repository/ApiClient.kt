package com.example.busbooking.domain.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

internal object ApiClient {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend inline fun <reified T> get(path: String): T = request("GET", path, null)
    suspend inline fun <reified Req, reified Res> post(path: String, body: Req): Res = request("POST", path, json.encodeToString(body))
    suspend inline fun <reified Req, reified Res> put(path: String, body: Req): Res = request("PUT", path, json.encodeToString(body))

    suspend inline fun <reified T> request(method: String, path: String, body: String?): T = withContext(Dispatchers.IO) {
        val baseUrls = ServerConfig.baseUrls()
        var lastFailure: Throwable? = null
        baseUrls.forEachIndexed { index, baseUrl ->
            try {
                return@withContext executeRequest<T>(baseUrl, method, path, body)
            } catch (e: Exception) {
                lastFailure = e
                if (e !is IOException || index == baseUrls.lastIndex) {
                    throw e
                }
            }
        }
        throw lastFailure ?: IOException("Khong the ket noi Spring web")
    }

    @PublishedApi
    internal inline fun <reified T> executeRequest(baseUrl: String, method: String, path: String, body: String?): T {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
        }
        if (body != null) {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use(BufferedReader::readText) }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) {
            throw IllegalStateException(parseMessage(text).ifBlank { "HTTP $code" })
        }
        return if (T::class == Unit::class) Unit as T else json.decodeFromString<T>(text)
    }

    fun parseMessage(text: String): String {
        return runCatching { json.decodeFromString<ApiError>(text).message }.getOrNull().orEmpty()
    }
}

@Serializable internal data class ApiError(val message: String = "")
@Serializable internal data class UserApi(val id: Long, val name: String, val email: String, val phone: String, val role: String, val isBlocked: Boolean = false, val createdAt: Long = 0)
@Serializable internal data class RouteApi(val id: Long, val origin: String, val destination: String, val distance: Int, val isActive: Boolean = true, val createdAt: Long = 0)
@Serializable internal data class BusApi(val id: Long, val busName: String, val totalSeats: Int, val licensePlate: String, val seatLayoutJson: String = "", val isActive: Boolean = true, val createdAt: Long = 0)
@Serializable internal data class TripApi(val id: Long, val routeId: Long, val busId: Long, val departureTime: Long, val arrivalTime: Long, val price: Double, val tripDate: Long, val status: String, val createdAt: Long = 0, val route: RouteApi, val bus: BusApi, val availableSeats: Int = 0)
@Serializable internal data class SeatApi(val id: Long, val busId: Long, val seatNumber: String, val floor: Int, val rowIndex: Int, val columnIndex: Int, val isWindow: Boolean = false, val isAisle: Boolean = false, val seatType: String = "NORMAL", val createdAt: Long = 0, val booked: Boolean = false)
@Serializable internal data class TicketApi(val id: Long, val userId: Long, val tripId: Long, val seatId: Long, val bookingTime: Long, val status: String, val cancellationReason: String? = null, val refundAmount: Double? = null, val refundStatus: String = "NONE", val paymentId: String? = null, val qrContent: String = "", val qrImageBase64: String = "", val qrMimeType: String = "")
@Serializable internal data class TicketDetailsApi(val ticket: TicketApi, val user: UserApi, val tripWithRouteAndBus: TripApi, val seat: SeatApi)
@Serializable internal data class RegisterRequest(val name: String, val email: String, val password: String, val phone: String)
@Serializable internal data class RegisterResponse(val id: Long)
@Serializable internal data class LoginRequest(val phone: String, val password: String)
@Serializable internal data class UpdateUserRequest(val name: String, val email: String, val phone: String)
@Serializable internal data class BookRequest(val userId: Long, val tripId: Long, val seatId: Long)
@Serializable internal data class BookResponse(val ticketId: Long, val paymentId: String)
@Serializable internal data class BookBatchSeat(val id: Long, val seatNumber: String)
@Serializable internal data class BookBatchSegment(val tripId: Long, val seats: List<BookBatchSeat>, val price: Double)
@Serializable internal data class BookBatchRequest(val userId: Long, val segments: List<BookBatchSegment>)
@Serializable internal data class BookBatchResponse(val ticketIds: List<Long>, val paymentId: String)
@Serializable internal data class CancelRequest(val userId: Long, val reason: String, val refundAmount: Double)
@Serializable internal data class RowsResponse(val rows: Int = 0)
@Serializable internal data class SeatAvailabilityApi(val totalSeats: Int, val bookedSeats: Int)


