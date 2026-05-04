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