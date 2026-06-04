package com.example.busbooking.data.relations

import com.example.busbooking.data.entity.Bus
import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.data.entity.Trip

data class BusWithSeats(val bus: Bus, val seats: List<Seat>)
data class RouteWithTrips(val route: Route, val trips: List<Trip>)

