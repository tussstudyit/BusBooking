package com.example.busbooking.staff.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class StaffFormattersTest {
    @Test
    fun scheduledTripBeforeDepartureShowsScheduled() {
        val label = StaffFormatters.tripStatus(
            value = "SCHEDULED",
            departureTime = 2_000L,
            now = 1_000L
        )

        assertEquals("Ch\u01b0a kh\u1edfi h\u00e0nh", label)
    }

    @Test
    fun scheduledTripAfterDepartureShowsDeparted() {
        val label = StaffFormatters.tripStatus(
            value = "SCHEDULED",
            departureTime = 1_000L,
            now = 2_000L
        )

        assertEquals("\u0110\u00e3 kh\u1edfi h\u00e0nh", label)
    }

    @Test
    fun finalTripStatusIsNotOverriddenByDepartureTime() {
        val label = StaffFormatters.tripStatus(
            value = "COMPLETED",
            departureTime = 1_000L,
            now = 2_000L
        )

        assertEquals("Ho\u00e0n th\u00e0nh", label)
    }
}
