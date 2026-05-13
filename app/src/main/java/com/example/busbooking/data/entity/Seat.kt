package com.example.busbooking.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Seat Entity - Production-like seat system
 *
 * Fields:
 * - id: Unique seat ID
 * - busId: Foreign key to Bus
 * - seatNumber: Display number (e.g., "A1", "B17")
 * - floor: Floor level (1 or 2)
 * - rowIndex: Row position (0-indexed)
 * - columnIndex: Column position (0-indexed)
 * - isWindow: Window seat indicator
 * - isAisle: Aisle seat indicator
 * - seatType: NORMAL, PREMIUM, DISABLED, AISLE_PASSAGE
 * - createdAt: Creation timestamp
 */
@Entity(
    tableName = "seats",
    foreignKeys = [
        ForeignKey(
            entity = Bus::class,
            parentColumns = ["id"],
            childColumns = ["busId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["busId", "seatNumber"], unique = true),
        Index(value = ["busId", "floor"]),
        Index(value = ["busId"])
    ]
)
data class Seat(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val busId: Long,
    val seatNumber: String,       // "A1", "A2", "B1", etc.
    val floor: Int = 1,           // 1 or 2
    val rowIndex: Int,            // 0-indexed row
    val columnIndex: Int,         // 0-indexed column
    val isWindow: Boolean = false, // Window seat
    val isAisle: Boolean = false,  // Aisle seat (normally not selectable)
    val seatType: String = "NORMAL", // NORMAL, PREMIUM, DISABLED, AISLE_PASSAGE
    val createdAt: Long = System.currentTimeMillis()
)