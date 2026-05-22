package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.Bus
import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.data.entity.Trip
import com.example.busbooking.data.relations.TripWithRouteAndBus
import com.example.busbooking.domain.models.SeatDisplay
import com.example.busbooking.domain.models.SeatStatus
import java.util.Calendar

object DemoBookingData {
    private const val BUS_ID = 9001L
    private const val TRIP_ID_BASE = 9001L

    private val demoRoutes = listOf(
        DemoRoute(1L, "Hà Nội", "Đà Nẵng", 765, 650_000.0, 13 * 3_600_000L),
        DemoRoute(2L, "Đà Nẵng", "TP. Hồ Chí Minh", 980, 850_000.0, 16 * 3_600_000L),
        DemoRoute(3L, "Hà Nội", "TP. Hồ Chí Minh", 1700, 1_200_000.0, 30 * 3_600_000L)
    ).flatMap { route ->
        listOf(
            route,
            route.copy(
                id = route.id + 100,
                origin = route.destination,
                destination = route.origin
            )
        )
    }

    private val defaultRoute = demoRoutes.first()

    private val bus = Bus(
        id = BUS_ID,
        busName = "Xe Demo BusBooking",
        totalSeats = 34,
        licensePlate = "DEMO-001"
    )

    fun locations(): Pair<List<String>, List<String>> {
        val provinces = demoRoutes
            .flatMap { listOf(it.origin, it.destination) }
            .distinct()
            .sorted()
        return provinces to provinces
    }

    fun tripsForDate(origin: String, destination: String, tripDate: Long): List<TripWithRouteAndBus> {
        val route = demoRoutes.firstOrNull {
            it.origin.equals(origin, ignoreCase = true) &&
                it.destination.equals(destination, ignoreCase = true)
        } ?: defaultRoute

        return tripsForRoute(route, tripDate)
    }

    fun tripsForDate(tripDate: Long): List<TripWithRouteAndBus> =
        tripsForRoute(defaultRoute, tripDate)

    fun tripById(tripId: Long): TripWithRouteAndBus {
        val todayTrips = demoRoutes.flatMap { tripsForRoute(it, System.currentTimeMillis()) }
        return todayTrips.firstOrNull { it.trip.id == tripId } ?: todayTrips.first()
    }

    fun seats(): List<SeatDisplay> = (1..34).map { index ->
        val floor = if (index <= 17) 1 else 2
        val floorIndex = if (floor == 1) index else index - 17
        SeatDisplay(
            seat = Seat(
                id = 90_000L + index,
                busId = BUS_ID,
                seatNumber = "${if (floor == 1) "A" else "B"}$floorIndex",
                floor = floor,
                rowIndex = (floorIndex - 1) / 3,
                columnIndex = (floorIndex - 1) % 3,
                isWindow = floorIndex % 3 != 2,
                isAisle = false,
                seatType = "STANDARD"
            ),
            status = SeatStatus.AVAILABLE
        )
    }

    private fun tripsForRoute(route: DemoRoute, tripDate: Long): List<TripWithRouteAndBus> {
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = tripDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return listOf(7, 15).mapIndexed { index, hour ->
            val departure = dayStart + hour * 3_600_000L
            TripWithRouteAndBus(
                trip = Trip(
                    id = TRIP_ID_BASE + route.id * 10 + index,
                    routeId = route.id,
                    busId = BUS_ID,
                    departureTime = departure,
                    arrivalTime = departure + route.durationMs,
                    price = route.price,
                    tripDate = dayStart,
                    status = "SCHEDULED"
                ),
                route = route.toRoute(),
                bus = bus
            )
        }
    }

    private fun DemoRoute.toRoute() = Route(
        id = id,
        origin = origin,
        destination = destination,
        distance = distance
    )

    private data class DemoRoute(
        val id: Long,
        val origin: String,
        val destination: String,
        val distance: Int,
        val price: Double,
        val durationMs: Long
    )
}
