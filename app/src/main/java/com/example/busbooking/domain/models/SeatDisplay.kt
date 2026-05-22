package com.example.busbooking.domain.models

import com.example.busbooking.data.entity.Seat

enum class SeatStatus {
    AVAILABLE,
    BOOKED
}

data class SeatDisplay(
    val seat: Seat,
    val status: SeatStatus
)
