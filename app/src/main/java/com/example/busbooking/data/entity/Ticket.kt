package com.example.busbooking.data.entity

data class Ticket(
    val id: Long = 0,
    val userId: Long,
    val tripId: Long,
    val seatId: Long,
    val bookingTime: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val cancellationReason: String? = null,
    val refundAmount: Double? = null,
    val refundStatus: String = "NONE",
    val paymentId: String? = null,
    val qrContent: String = "",
    val qrImageBase64: String = "",
    val qrMimeType: String = ""
)

object TicketStatus {
    const val PENDING = "PENDING"
    const val PENDING_PAYMENT = "PENDING_PAYMENT"
    const val CONFIRMED = "CONFIRMED"
    const val CHECKED_IN = "CHECKED_IN"
    const val USED = "USED"
    const val CANCELLED = "CANCELLED"
}

object RefundStatus {
    const val NONE = "NONE"
    const val PENDING = "PENDING"
    const val COMPLETED = "COMPLETED"
}

