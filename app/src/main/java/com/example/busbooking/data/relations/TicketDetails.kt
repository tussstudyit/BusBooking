package com.example.busbooking.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.busbooking.data.entity.*

/**
 * TicketDetails - Complete ticket information with all related entities
 *
 * This relation combines:
 * - Ticket: The main booking record (status, dates, refund info)
 * - User: The passenger information
 * - TripWithRouteAndBus: Complete trip details including route and bus
 * - Seat: The purchased seat information
 *
 * Used by:
 * - TicketDAO for ticket retrieval and display
 * - My bookings screen
 * - Booking confirmation/details screen
 * - Admin ticket management
 */
data class TicketDetails(
    @Embedded
    val ticket: Ticket,

    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val user: User,

    @Relation(
        parentColumn = "tripId",
        entityColumn = "id",
        entity = Trip::class
    )
    val tripWithRouteAndBus: TripWithRouteAndBus,

    @Relation(
        parentColumn = "seatId",
        entityColumn = "id"
    )
    val seat: Seat
)

fun TicketDetails.tripScheduleMillis(): Long {
    val trip = tripWithRouteAndBus.trip
    return when {
        trip.departureTime > 0L -> trip.departureTime
        trip.tripDate > 0L -> trip.tripDate
        else -> 0L
    }
}

fun TicketDetails.hasTripDeparted(now: Long = System.currentTimeMillis()): Boolean {
    val scheduleMillis = tripScheduleMillis()
    return scheduleMillis > 0L && scheduleMillis <= now
}

fun TicketDetails.isUpcomingTicket(now: Long = System.currentTimeMillis()): Boolean {
    return ticket.status in UPCOMING_TICKET_STATUSES && !hasTripDeparted(now)
}

fun TicketDetails.isTicketHistory(now: Long = System.currentTimeMillis()): Boolean {
    return when (ticket.status) {
        "PENDING", "PENDING_PAYMENT" -> false
        "CONFIRMED" -> hasTripDeparted(now)
        "USED", "CANCELLED", "PAYMENT_FAILED" -> true
        else -> hasTripDeparted(now)
    }
}

private val UPCOMING_TICKET_STATUSES = setOf("CONFIRMED", "PENDING", "PENDING_PAYMENT")
