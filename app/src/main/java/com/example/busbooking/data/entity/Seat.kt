package com.example.busbooking.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


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
        Index(value = ["busId"])
    ]
)
data class Seat(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val busId: Long,
    val seatNumber: String, // "1A", "2B"
    val isPremium: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)