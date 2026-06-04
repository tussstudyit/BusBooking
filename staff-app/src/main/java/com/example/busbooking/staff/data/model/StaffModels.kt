package com.example.busbooking.staff.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StaffUser(
    val id: Long,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val role: String,
    val companyName: String = ""
) {
    val canUseStaffApp: Boolean get() = role.equals("STAFF", true)
}

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val user: StaffUser,
    val token: String = ""
)

@Serializable
data class StaffHomeSummary(
    val staffName: String,
    val companyName: String,
    val assignedTripsToday: Int,
    val checkedInPassengers: Int,
    val bookedPassengersToday: Int = 0,
    val todayTrips: List<StaffTripSummary> = emptyList()
)

@Serializable
data class StaffTripSummary(
    val id: Long,
    val code: String,
    val origin: String,
    val destination: String,
    val departureTime: Long,
    val licensePlate: String,
    val totalSeats: Int,
    val bookedSeats: Int,
    val status: String
)

@Serializable
data class StaffTripDetail(
    val id: Long,
    val code: String,
    val origin: String,
    val destination: String,
    val departureTime: Long,
    val licensePlate: String,
    val totalSeats: Int,
    val bookedSeats: Int,
    val status: String,
    val driverName: String = "",
    val staffName: String = "",
    val passengers: List<StaffPassenger> = emptyList(),
    val seats: List<StaffSeat> = emptyList()
)

@Serializable
data class StaffPassenger(
    val ticketId: Long,
    val name: String,
    val phone: String,
    val seatNumber: String,
    val paymentStatus: String,
    val checkInStatus: String
)

@Serializable
data class StaffSeat(
    val seatId: Long,
    val seatNumber: String,
    val floor: Int,
    val rowIndex: Int,
    val columnIndex: Int,
    val status: String,
    val passenger: StaffPassenger? = null
)

@Serializable
data class TicketVerifyRequest(
    val qrContent: String,
    val staffId: Long
)

@Serializable
data class TicketCheckInRequest(
    val staffId: Long
)

@Serializable
data class TicketVerificationResult(
    val valid: Boolean,
    val message: String = "",
    val ticketId: Long = 0,
    val tripId: Long = 0,
    val passengerName: String = "",
    val phone: String = "",
    val origin: String = "",
    val destination: String = "",
    val departureTime: Long = 0,
    val seatNumber: String = "",
    val paymentStatus: String = "",
    val checkInStatus: String = ""
)

@Serializable
data class ApiMessage(
    val message: String = ""
)
