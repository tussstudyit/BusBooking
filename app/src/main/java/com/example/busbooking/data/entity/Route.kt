package com.example.busbooking.data.entity

data class Route(
    val id: Long = 0,
    val origin: String,
    val destination: String,
    val distance: Int,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

