package com.example.busbooking.test

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.runners.AndroidJUnit4
import org.junit.Rule
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat

// ════════════════════════════════════════════════════════════════════════════════
// BASE TEST CLASSES
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Base class for all DAO tests using in-memory Room database
 */
abstract class DaoTestBase {
    protected lateinit var database: BusBookingDatabase

    @Before
    fun setupDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BusBookingDatabase::class.java
        )
        .allowMainThreadQueries()  // Safe for testing
        .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }
}

/**
 * Base class for all ViewModel tests
 */
abstract class ViewModelTestBase {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
}

// ════════════════════════════════════════════════════════════════════════════════
// DAO TESTS
// ════════════════════════════════════════════════════════════════════════════════

@RunWith(AndroidJUnit4::class)
class TicketDAOTest : DaoTestBase() {

    private lateinit var ticketDAO: TicketDAO
    private lateinit var tripDAO: TripDAO
    private lateinit var userDAO: UserDAO
    private lateinit var busDAO: BusDAO
    private lateinit var seatDAO: SeatDAO

    @Before
    override fun setupDatabase() {
        super.setupDatabase()
        ticketDAO = database.ticketDao()
        tripDAO = database.tripDao()
        userDAO = database.userDao()
        busDAO = database.busDao()
        seatDAO = database.seatDao()
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TEST 1: Atomic Booking - Only 1 Booking Succeeds ⭐ CRITICAL
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testBookTicket_ConcurrentBooking_OnlyOneSucceeds() = runTest {
        // Setup: Create test data
        val route = routineSetupData()
        val trip = route.trips[0]
        val seat = route.seats[0]
        val user1 = User(id = 1L, name = "User1", email = "user1@test.com", role = "USER")
        val user2 = User(id = 2L, name = "User2", email = "user2@test.com", role = "USER")

        userDAO.insertUser(user1)
        userDAO.insertUser(user2)

        // Test: Launch 2 concurrent booking requests
        val results = coroutineScope {
            val result1 = async {
                try {
                    ticketDAO.bookTicket(
                        Ticket(
                            userId = user1.id,
                            tripId = trip.id,
                            seatId = seat.id,
                            status = "CONFIRMED"
                        )
                    )
                } catch (e: Exception) {
                    -1L  // Indicate failure
                }
            }

            val result2 = async {
                try {
                    ticketDAO.bookTicket(
                        Ticket(
                            userId = user2.id,
                            tripId = trip.id,
                            seatId = seat.id,
                            status = "CONFIRMED"
                        )
                    )
                } catch (e: Exception) {
                    -1L  // Indicate failure
                }
            }

            listOf(result1.await(), result2.await())
        }

        // Verify: Only 1 succeeded
        val successCount = results.count { it > 0 }
        assertThat(successCount).isEqualTo(1)

        // Verify: Seat is booked
        val bookedSeats = seatDAO.getBookedSeatsForTrip(trip.id)
        assertThat(bookedSeats).contains(seat.id)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TEST 2: Cancel Ticket - Status Updated
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testCancelTicket_StatusUpdatedToCancelled() = runTest {
        // Setup
        val trip = routineSetupData().trips[0]
        val user = User(id = 1L, name = "User", email = "user@test.com", role = "USER")
        userDAO.insertUser(user)

        val ticketId = ticketDAO.bookTicket(
            Ticket(
                userId = user.id,
                tripId = trip.id,
                seatId = trip.seats[0].id,
                status = "CONFIRMED"
            )
        )

        // Test: Cancel ticket
        ticketDAO.cancelTicket(ticketId, "Changed my plans")

        // Verify: Status changed
        val ticket = ticketDAO.getTicketById(ticketId)
        assertThat(ticket?.status).isEqualTo("CANCELLED")
        assertThat(ticket?.cancelReason).isEqualTo("Changed my plans")
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TEST 3: Search Trips - Filter by Route & Date
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testGetTripsForRouteAndDate_ReturnsCorrectTrips() = runTest {
        // Setup: Create 5 trips
        val route = routineSetupData()
        val date1 = "2026-05-03"
        val date2 = "2026-05-04"

        // Add 3 trips on date1, 2 on date2
        val trip1 = Trip(routeId = route.id, date = date1, departureTime = "08:00" /* ... */)
        val trip2 = Trip(routeId = route.id, date = date1, departureTime = "14:00" /* ... */)
        val trip3 = Trip(routeId = route.id, date = date1, departureTime = "20:00" /* ... */)
        val trip4 = Trip(routeId = route.id, date = date2, departureTime = "08:00" /* ... */)
        val trip5 = Trip(routeId = route.id, date = date2, departureTime = "14:00" /* ... */)

        tripDAO.insertTrips(listOf(trip1, trip2, trip3, trip4, trip5))

        // Test: Search for trips on date1
        val trips = tripDAO.getTripsForRouteAndDate(route.id, date1)

        // Verify: Only 3 trips on date1
        assertThat(trips).hasSize(3)
        assertThat(trips.map { it.date }).containsOnly(date1)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TEST 4: Statistics - Calculate Revenue
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testGetStatistics_CalculatesRevenueByDate() = runTest {
        // Setup
        val route = routineSetupData()
        val trip1 = Trip(routeId = route.id, date = "2026-05-03", price = 1500.0 /* ... */)
        val trip2 = Trip(routeId = route.id, date = "2026-05-04", price = 1200.0 /* ... */)

        tripDAO.insertTrips(listOf(trip1, trip2))

        // Create bookings
        val user = User(id = 1L, name = "User", email = "user@test.com", role = "USER")
        userDAO.insertUser(user)

        // Book 2 seats on trip1 (revenue = 3000)
        ticketDAO.bookTicket(Ticket(userId = user.id, tripId = trip1.id, seatId = 1L))
        ticketDAO.bookTicket(Ticket(userId = user.id, tripId = trip1.id, seatId = 2L))

        // Book 1 seat on trip2 (revenue = 1200)
        ticketDAO.bookTicket(Ticket(userId = user.id, tripId = trip2.id, seatId = 1L))

        // Test: Get statistics
        val stats = ticketDAO.getStatisticsByDateRange("2026-05-03", "2026-05-04")

        // Verify
        assertThat(stats.totalRevenue).isEqualTo(4200.0)
        assertThat(stats.totalBookings).isEqualTo(3)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helper: Setup test data
    // ────────────────────────────────────────────────────────────────────────────

    private suspend fun routineSetupData(): Route {
        // Create route
        val route = Route(id = 1L, origin = "Delhi", destination = "Mumbai", distance = 1400)
        database.routeDao().insertRoute(route)

        // Create bus
        val bus = Bus(id = 1L, busName = "Shatabdi", totalSeats = 20, licensePlate = "DL-01-AA-1234")
        busDAO.insertBus(bus)

        // Create seats
        val seats = (1..20).map { i ->
            Seat(id = i.toLong(), busId = bus.id, seatNumber = "A$i", status = "AVAILABLE")
        }
        seatDAO.insertSeats(seats)

        // Create trip
        val trip = Trip(
            id = 1L,
            routeId = route.id,
            busId = bus.id,
            departureTime = "18:00",
            arrivalTime = "08:00",
            price = 1500.0,
            date = "2026-05-03"
        )
        tripDAO.insertTrip(trip)

        return route
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// VIEWMODEL TESTS
// ════════════════════════════════════════════════════════════════════════════════

@RunWith(AndroidJUnit4::class)
class LoginViewModelTest : ViewModelTestBase() {

    // ────────────────────────────────────────────────────────────────────────────
    // TEST 1: Registration Validation
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testRegisterViewModel_EmptyEmail_ShowsValidationError() = runTest {
        val mockRepository = mockAuthRepository()
        val viewModel = RegisterViewModel(mockRepository)

        // Collect state emissions
        val states = mutableListOf<UIState>()
        val job = launch {
            viewModel.uiState.collect { states.add(it) }
        }

        // Try to register with empty email
        viewModel.register(
            name = "Rajesh",
            email = "",  // Invalid
            password = "Pass@123",
            phone = "+919876543210"
        )

        advanceUntilIdle()

        // Verify: ValidationError state
        assertThat(states).contains {
            it is UIState.ValidationError && it.errors.containsKey("email")
        }

        job.cancel()
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TEST 2: Login Success Flow
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testLoginViewModel_CorrectCredentials_EmitsSuccess() = runTest {
        val mockRepository = mockAuthRepository()
        val mockSession = mockSessionManager()
        val viewModel = LoginViewModel(mockRepository, mockSession)

        // Collect state emissions
        val states = mutableListOf<UIState>()
        val job = launch {
            viewModel.uiState.collect { states.add(it) }
        }

        // Login
        viewModel.login("rajesh@gmail.com", "Pass@123")
        advanceUntilIdle()

        // Verify state sequence
        assertThat(states).hasSize(3)
        assertThat(states[0]).isInstanceOf(UIState.Loading::class.java)
        assertThat(states[1]).isInstanceOf(UIState.Success::class.java)

        job.cancel()
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TEST 3: Booking ViewModel - Atomic Safety
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun testBookingViewModel_ConcurrentBooking_ShowsAlreadyBookedError() = runTest {
        val mockRepository = mockTicketRepository()
        val viewModel = BookingConfirmationViewModel(mockRepository)

        // Collect state emissions
        val states = mutableListOf<UIState>()
        val job = launch {
            viewModel.uiState.collect { states.add(it) }
        }

        // Try booking (will trigger concurrent failure scenario)
        viewModel.confirmBooking(tripId = 1L, seatId = 1L)
        advanceUntilIdle()

        // Verify: Error state (seat already booked)
        val errorState = states.findLast { it is UIState.Error }
        assertThat(errorState?.message).contains("already booked")

        job.cancel()
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// MOCK SETUP HELPERS
// ════════════════════════════════════════════════════════════════════════════════

fun mockAuthRepository(): AuthRepository {
    // Create mock that returns success
    val mock = mock<AuthRepository>()
    whenever(mock.login(anyString(), anyString()))
        .thenReturn(Result.Success(UserData(id = 1L, email = "user@test.com", role = "USER")))
    return mock
}

fun mockSessionManager(): ISessionManager {
    val mock = mock<ISessionManager>()
    whenever(mock.saveSession(any())).thenReturn(Unit)
    whenever(mock.isAdminUser()).thenReturn(false)
    return mock
}

fun mockTicketRepository(): TicketRepository {
    val mock = mock<TicketRepository>()
    whenever(mock.bookTicket(any(), any(), any()))
        .thenReturn(Result.Error(Exception("Seat already booked")))
    return mock
}

// ════════════════════════════════════════════════════════════════════════════════
// COROUTINE TEST RULE
// ════════════════════════════════════════════════════════════════════════════════

class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// USAGE NOTES
// ════════════════════════════════════════════════════════════════════════════════

/**
 * To run tests:
 *
 * Run all tests:
 *   ./gradlew test
 *
 * Run specific test class:
 *   ./gradlew testDebugUnitTest --tests TicketDAOTest
 *
 * Run specific test method:
 *   ./gradlew testDebugUnitTest --tests TicketDAOTest.testBookTicket_ConcurrentBooking_OnlyOneSucceeds
 *
 * Generate coverage report:
 *   ./gradlew testDebugUnitTestCoverage
 *   Report: app/build/reports/coverage/debug/index.html
 */

