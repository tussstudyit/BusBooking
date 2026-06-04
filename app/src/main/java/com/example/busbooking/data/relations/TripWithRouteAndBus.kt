package com.example.busbooking.data.relations

import com.example.busbooking.data.entity.Bus
import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.entity.Trip

data class TripWithRouteAndBus(
    val trip: Trip,
    val route: Route,
    val bus: Bus
)

