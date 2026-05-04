package com.example.busbooking.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tickets",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Trip::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Seat::class, parentColumns = ["id"], childColumns = ["seatId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["tripId", "status"]),
        Index(value = ["status"]),
        Index(value = ["bookingTime"]),
        Index(value = ["tripId", "seatId"], unique = true),
        Index(value = ["seatId"])
    ]
)
data class Ticket(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val tripId: Long,
    val seatId: Long,
    val bookingTime: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, CONFIRMED, USED, CANCELLED
    val cancellationReason: String? = null,
    val refundAmount: Double? = null,
    val refundStatus: String = "NONE" // NONE, PENDING, COMPLETED
)

object TicketStatus {
    const val PENDING = "PENDING"
    const val CONFIRMED = "CONFIRMED"
    const val USED = "USED"
    const val CANCELLED = "CANCELLED"
}

object RefundStatus {
    const val NONE = "NONE"
    const val PENDING = "PENDING"
    const val COMPLETED = "COMPLETED"
}
