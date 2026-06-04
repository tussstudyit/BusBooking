package com.example.busbooking.staff.utils

import com.example.busbooking.staff.data.model.StaffHomeSummary
import com.example.busbooking.staff.data.model.StaffTripSummary
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private const val MAX_HOME_TODAY_TRIPS = 2
private val vnTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Bangkok")

fun List<StaffTripSummary>.homeTodayTrips(now: Long = System.currentTimeMillis()): List<StaffTripSummary> {
    val todayStart = dayStart(now)
    val todayEnd = todayStart + 86_400_000L
    return sortedBy { it.departureTime }
        .filter { it.departureTime in todayStart..<todayEnd }
        .filterNot { it.isPastTrip(now) }
        .take(MAX_HOME_TODAY_TRIPS)
}

fun StaffHomeSummary.todayOnlySummary(now: Long = System.currentTimeMillis()): StaffHomeSummary {
    val filteredTrips = todayTrips.homeTodayTrips(now)
    val bookedPassengers = filteredTrips.sumOf { it.bookedSeats }
    return copy(
        assignedTripsToday = filteredTrips.size,
        checkedInPassengers = checkedInPassengers.coerceAtMost(bookedPassengers),
        bookedPassengersToday = bookedPassengers,
        todayTrips = filteredTrips
    )
}

data class StaffTripGroups(
    val upcomingTrips: List<StaffTripSummary>,
    val historyTrips: List<StaffTripSummary>
)

fun List<StaffTripSummary>.groupForStaffTripList(now: Long = System.currentTimeMillis()): StaffTripGroups {
    val orderedTrips = sortedBy { it.departureTime }
    return StaffTripGroups(
        upcomingTrips = orderedTrips.filterNot { it.isPastTrip(now) },
        historyTrips = orderedTrips.filter { it.isPastTrip(now) }.sortedByDescending { it.departureTime }
    )
}

private fun StaffTripSummary.isPastTrip(now: Long): Boolean {
    val normalizedStatus = status.uppercase(Locale.ROOT)
    return departureTime <= now || normalizedStatus in setOf("COMPLETED", "CANCELLED")
}

private fun dayStart(millis: Long): Long {
    return Calendar.getInstance(vnTimeZone).apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
