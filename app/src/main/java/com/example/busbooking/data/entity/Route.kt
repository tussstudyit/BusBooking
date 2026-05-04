package com.example.busbooking.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Route Entity - Represents bus routes between cities
 *
 * Indices:
 * - (origin, destination): For fast route search
 * - origin & destination: Individual location lookups
 */
@Entity(
    tableName = "routes",
    indices = [
        Index(value = ["origin", "destination"]),
        Index(value = ["origin"]),
        Index(value = ["destination"])
    ]
)
data class Route(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val origin: String,
    val destination: String,
    val distance: Int, // in km
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

