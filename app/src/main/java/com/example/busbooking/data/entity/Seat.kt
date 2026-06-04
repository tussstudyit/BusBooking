package com.example.busbooking.data.entity

data class Seat(
    val id: Long = 0,
    val busId: Long,
    val seatNumber: String,
    val floor: Int = 1,
    val rowIndex: Int,
    val columnIndex: Int,
    val isWindow: Boolean = false,
    val isAisle: Boolean = false,
    val seatType: String = "NORMAL",
    val createdAt: Long = System.currentTimeMillis()
)

