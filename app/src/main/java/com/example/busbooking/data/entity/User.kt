package com.example.busbooking.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User Entity - Stores user account information
 *
 * Indices:
 * - email: For fast login & duplicate prevention (UNIQUE)
 * - phone: For user lookup
 * - role: For role-based queries (admin list, user list)
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["phone"]),
        Index(value = ["role"])
    ]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String,
    val password: String,
    val phone: String,
    val role: String, // "USER" or "ADMIN"
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

object UserRole {
    const val USER = "USER"
    const val ADMIN = "ADMIN"
}
