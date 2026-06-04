package com.example.busbooking.data.entity

data class User(
    val id: Long = 0,
    val name: String,
    val email: String,
    val password: String = "",
    val phone: String,
    val role: String,
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

object UserRole {
    const val USER = "USER"
}

