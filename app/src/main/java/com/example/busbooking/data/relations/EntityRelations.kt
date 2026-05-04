package com.example.busbooking.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.busbooking.data.entity.*

// ================= BUS =================

/**
 * BusWithSeats - Bus entity with all its seats
 * Used in: BusDAO.getBusWithSeats() for seat selection screen
 */
data class BusWithSeats(
    @Embedded
    val bus: Bus,

    @Relation(
        parentColumn = "id",
        entityColumn = "busId"
    )
    val seats: List<Seat>
)

// ================= ROUTE =================

/**
 * RouteWithTrips - Route entity with all its trips
 * Used in: RouteDAO.getRouteWithTrips() for route details with trips listing
 */
data class RouteWithTrips(
    @Embedded
    val route: Route,

    @Relation(
        parentColumn = "id",
        entityColumn = "routeId"
    )
    val trips: List<Trip>
)