package com.example.busbooking.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.busbooking.data.entity.*

/**
 * TripWithRouteAndBus - A complete trip view with route and bus information
 *
 * This relation combines:
 * - Trip: The main trip entity (departure/arrival times, price, etc.)
 * - Route: The route details (origin, destination, distance)
 * - Bus: The bus information (name, total seats, license plate)
 *
 * Used by:
 * - Trip browsing/search screens
 * - Seat selection (need bus info for seats)
 * - Trip details display
 * - Booking confirmation
 */
data class TripWithRouteAndBus(

    @Embedded
    val trip: Trip,

    @Relation(
        parentColumn = "routeId",
        entityColumn = "id"
    )
    val route: Route,

    @Relation(
        parentColumn = "busId",
        entityColumn = "id"
    )
    val bus: Bus
)