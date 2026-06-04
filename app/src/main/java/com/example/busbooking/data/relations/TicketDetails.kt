package com.example.busbooking.data.relations

import com.example.busbooking.data.entity.Seat
import com.example.busbooking.data.entity.Ticket
import com.example.busbooking.data.entity.User

data class TicketDetails(
    val ticket: Ticket,
    val user: User,
    val tripWithRouteAndBus: TripWithRouteAndBus,
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

private val UPCOMING_TICKET_STATUSES = setOf("CONFIRMED", "CHECKED_IN", "PENDING", "PENDING_PAYMENT")

