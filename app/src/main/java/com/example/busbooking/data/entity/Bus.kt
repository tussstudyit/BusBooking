package com.example.busbooking.data.entity

data class Bus(
    val id: Long = 0,
    val busName: String,
    val totalSeats: Int,
    val licensePlate: String,
    val seatLayoutJson: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

