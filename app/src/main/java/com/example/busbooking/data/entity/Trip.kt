package com.example.busbooking.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "trips",
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Bus::class,
            parentColumns = ["id"],
            childColumns = ["busId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["routeId", "tripDate"]),
        Index(value = ["busId", "tripDate"]),
        Index(value = ["departureTime"]),
        Index(value = ["status"])
    ]
)
data class Trip(
    @PrimaryKey(autoGenerate = true)
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