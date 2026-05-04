package com.example.busbooking.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bus Entity - Stores bus information
 *
 * Fields:
 * - id: Unique identifier (auto-generated)
 * - busName: Bus name/identifier (e.g., "Bus-001")
 * - totalSeats: Total number of seats
 * - licensePlate: Unique license plate (UNIQUE constraint enforced)
 * - seatLayoutJson: JSON serialized seat layout (optional, for UI visualization)
 * - isActive: Soft delete flag
 * - createdAt: Bus creation timestamp
 *
 * Indices:
 * - licensePlate: UNIQUE for registration number lookup
 * - busName: For bus management
 */
@Entity(
    tableName = "buses",
    indices = [
        Index(value = ["licensePlate"], unique = true),
        Index(value = ["busName"])
    ]
)
data class Bus(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val busName: String,
    val totalSeats: Int,
    val licensePlate: String,
    val seatLayoutJson: String = "", // JSON serialized seat layout
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

