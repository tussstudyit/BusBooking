package com.example.busbooking.data.entity

data class Trip(
    val id: Long = 0,
    val routeId: Long,
    val busId: Long,
    val departureTime: Long,
    val arrivalTime: Long,
    val price: Double,
    val tripDate: Long,
    val status: String = TripStatus.SCHEDULED,
    val createdAt: Long = System.currentTimeMillis()
)

object TripStatus {
    const val SCHEDULED = "SCHEDULED"
    const val DEPARTED = "DEPARTED"
    const val COMPLETED = "COMPLETED"
    const val CANCELLED = "CANCELLED"
}

