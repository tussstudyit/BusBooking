package com.example.busbooking.presentation.ui.state

import com.example.busbooking.data.entity.User
import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.entity.Bus
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.data.relations.TripWithRouteAndBus
import com.example.busbooking.data.relations.TicketDetails

/**
 * ========================================================================
 * USER SCREENS - UIState Models
 * ========================================================================
 */

// ========================================================================
// SPLASH SCREEN
// ========================================================================
sealed class SplashUIState {
    object Loading : SplashUIState()
    object NavigateToHome : SplashUIState()
    object NavigateToLogin : SplashUIState()
}

// ========================================================================
// LOGIN SCREEN
// ========================================================================
sealed class LoginUIState {
    object Idle : LoginUIState()
    object Loading : LoginUIState()
    data class Success(val user: User) : LoginUIState()
    data class Error(val message: String, val fieldError: String? = null) : LoginUIState()
}

sealed class LoginEvent {
    data class NavigateToHome(val userId: Long) : LoginEvent()
    data class ShowToast(val message: String) : LoginEvent()
    data class NavigateToRegister : LoginEvent()
}

// ========================================================================
// REGISTER SCREEN
// ========================================================================
sealed class RegisterUIState {
    object Idle : RegisterUIState()
    object Loading : RegisterUIState()
    data class Success(val userId: Long) : RegisterUIState()
    data class ValidationError(val errors: Map<String, String>) : RegisterUIState()
    data class Error(val message: String) : RegisterUIState()
}

sealed class RegisterEvent {
    data class NavigateToLogin : RegisterEvent()
    data class ShowToast(val message: String) : RegisterEvent()
}

// ========================================================================
// TRIP SEARCH SCREEN
// ========================================================================
sealed class TripSearchUIState {
    object Idle : TripSearchUIState()
    object LoadingOrigins : TripSearchUIState()
    object LoadingDestinations : TripSearchUIState()
    object LoadingTrips : TripSearchUIState()
    data class OriginsLoaded(val origins: List<String>) : TripSearchUIState()
    data class DestinationsLoaded(val destinations: List<String>) : TripSearchUIState()
    data class TripsLoaded(val trips: List<TripWithRouteAndBus>) : TripSearchUIState()
    data class ValidationError(val message: String) : TripSearchUIState()
    data class Error(val message: String) : TripSearchUIState()
}

sealed class TripSearchEvent {
    data class NavigateToSeatSelection(val tripId: Long) : TripSearchEvent()
    data class ShowToast(val message: String) : TripSearchEvent()
}

// ========================================================================
// SEAT SELECTION SCREEN
// ========================================================================
sealed class SeatSelectionUIState {
    object Loading : SeatSelectionUIState()
    data class Loaded(
        val seats: List<Seat>,
        val selectedSeatId: Long? = null,
        val selectedSeatNumber: String? = null
    ) : SeatSelectionUIState()
    data class Error(val message: String) : SeatSelectionUIState()
}

sealed class SeatSelectionEvent {
    data class NavigateToConfirmation(val tripId: Long, val seatId: Long) : SeatSelectionEvent()
    object NavigateBack : SeatSelectionEvent()
}

// ========================================================================
// BOOKING CONFIRMATION SCREEN
// ========================================================================
sealed class BookingConfirmationUIState {
    object Idle : BookingConfirmationUIState()
    object Loading : BookingConfirmationUIState()
    data class Loaded(
        val trip: TripWithRouteAndBus,
        val seatNumber: String,
        val totalPrice: Double
    ) : BookingConfirmationUIState()
    object BookingSuccess : BookingConfirmationUIState()
    data class BookingFailed(val message: String) : BookingConfirmationUIState()
    object SeatAlreadyBooked : BookingConfirmationUIState()
    data class Error(val message: String) : BookingConfirmationUIState()
}

sealed class BookingConfirmationEvent {
    data class NavigateToTicketDetail(val ticketId: Long) : BookingConfirmationEvent()
    data class ShowToast(val message: String) : BookingConfirmationEvent()
    object NavigateBackToSearch : BookingConfirmationEvent()
}

// ========================================================================
// MY TICKETS SCREEN (Active Bookings)
// ========================================================================
sealed class MyTicketsUIState {
    object Loading : MyTicketsUIState()
    data class Loaded(val tickets: List<TicketDetails>) : MyTicketsUIState()
    object Empty : MyTicketsUIState()
    data class Error(val message: String) : MyTicketsUIState()
}

sealed class MyTicketsEvent {
    data class NavigateToDetail(val ticketId: Long) : MyTicketsEvent()
    data class ShowToast(val message: String) : MyTicketsEvent()
}

// ========================================================================
// TICKET HISTORY SCREEN
// ========================================================================
sealed class TicketHistoryUIState {
    object Loading : TicketHistoryUIState()
    data class Loaded(val tickets: List<TicketDetails>) : TicketHistoryUIState()
    object Empty : TicketHistoryUIState()
    data class Error(val message: String) : TicketHistoryUIState()
}

sealed class TicketHistoryEvent {
    data class NavigateToDetail(val ticketId: Long) : TicketHistoryEvent()
    data class ShowToast(val message: String) : TicketHistoryEvent()
}

// ========================================================================
// TICKET DETAIL SCREEN
// ========================================================================
sealed class TicketDetailUIState {
    object Loading : TicketDetailUIState()
    data class Loaded(val ticket: TicketDetails) : TicketDetailUIState()
    data class Error(val message: String) : TicketDetailUIState()
}

sealed class TicketDetailEvent {
    data class NavigateToCancel(val ticketId: Long) : TicketDetailEvent()
    object NavigateBack : TicketDetailEvent()
}

// ========================================================================
// CANCEL TICKET SCREEN
// ========================================================================
sealed class CancelTicketUIState {
    object Idle : CancelTicketUIState()
    object Loading : CancelTicketUIState()
    data class Loaded(
        val ticket: TicketDetails,
        val refundAmount: Double
    ) : CancelTicketUIState()
    object CancellationSuccess : CancelTicketUIState()
    data class ValidationError(val message: String) : CancelTicketUIState()
    data class Error(val message: String) : CancelTicketUIState()
}

sealed class CancelTicketEvent {
    data class ShowConfirmation(val message: String) : CancelTicketEvent()
    data class NavigateToHistory : CancelTicketEvent()
}

// ========================================================================
// USER PROFILE SCREEN
// ========================================================================
sealed class UserProfileUIState {
    object Loading : UserProfileUIState()
    data class Loaded(val user: User) : UserProfileUIState()
    object UpdateSuccess : UserProfileUIState()
    data class ValidationError(val errors: Map<String, String>) : UserProfileUIState()
    data class Error(val message: String) : UserProfileUIState()
}

sealed class UserProfileEvent {
    data class ShowToast(val message: String) : UserProfileEvent()
    object NavigateToLogin : UserProfileEvent()
}

/**
 * ========================================================================
 * ADMIN SCREENS - UIState Models
 * ========================================================================
 */

// ========================================================================
// ADMIN DASHBOARD
// ========================================================================
sealed class AdminDashboardUIState {
    object Loading : AdminDashboardUIState()
    data class Loaded(
        val totalRevenue: Double,
        val totalBookings: Long,
        val confirmedTickets: Long,
        val averagePrice: Double,
        val totalRefunds: Double
    ) : AdminDashboardUIState()
    data class Error(val message: String) : AdminDashboardUIState()
}

// ========================================================================
// ROUTE MANAGEMENT
// ========================================================================
sealed class RouteManagementUIState {
    object Loading : RouteManagementUIState()
    data class Loaded(val routes: List<Route>) : RouteManagementUIState()
    object Empty : RouteManagementUIState()
    object CreationSuccess : RouteManagementUIState()
    object UpdateSuccess : RouteManagementUIState()
    object DeletionSuccess : RouteManagementUIState()
    data class ValidationError(val errors: Map<String, String>) : RouteManagementUIState()
    data class Error(val message: String) : RouteManagementUIState()
}

sealed class RouteManagementEvent {
    data class NavigateToCreate : RouteManagementEvent()
    data class NavigateToEdit(val routeId: Long) : RouteManagementEvent()
    data class ShowToast(val message: String) : RouteManagementEvent()
}

// ========================================================================
// BUS MANAGEMENT
// ========================================================================
sealed class BusManagementUIState {
    object Loading : BusManagementUIState()
    data class Loaded(val buses: List<Bus>) : BusManagementUIState()
    object Empty : BusManagementUIState()
    object CreationSuccess : BusManagementUIState()
    object UpdateSuccess : BusManagementUIState()
    object DeletionSuccess : BusManagementUIState()
    data class ValidationError(val errors: Map<String, String>) : BusManagementUIState()
    data class Error(val message: String) : BusManagementUIState()
}

sealed class BusManagementEvent {
    data class NavigateToCreate : BusManagementEvent()
    data class NavigateToEdit(val busId: Long) : BusManagementEvent()
    data class NavigateToSeats(val busId: Long) : BusManagementEvent()
    data class ShowToast(val message: String) : BusManagementEvent()
}

// ========================================================================
// TRIP MANAGEMENT
// ========================================================================
sealed class TripManagementUIState {
    object Loading : TripManagementUIState()
    data class Loaded(val trips: List<TripWithRouteAndBus>) : TripManagementUIState()
    object Empty : TripManagementUIState()
    object CreationSuccess : TripManagementUIState()
    object UpdateSuccess : TripManagementUIState()
    object CancellationSuccess : TripManagementUIState()
    data class ValidationError(val errors: Map<String, String>) : TripManagementUIState()
    data class Error(val message: String) : TripManagementUIState()
}

sealed class TripManagementEvent {
    data class NavigateToCreate : TripManagementEvent()
    data class NavigateToEdit(val tripId: Long) : TripManagementEvent()
    data class NavigateToTickets(val tripId: Long) : TripManagementEvent()
    data class ShowToast(val message: String) : TripManagementEvent()
}

// ========================================================================
// SEAT MANAGEMENT
// ========================================================================
sealed class SeatManagementUIState {
    object Loading : SeatManagementUIState()
    data class Loaded(val seats: List<Seat>) : SeatManagementUIState()
    object Empty : SeatManagementUIState()
    object CreationSuccess : SeatManagementUIState()
    object BlockSuccess : SeatManagementUIState()
    data class Error(val message: String) : SeatManagementUIState()
}

sealed class SeatManagementEvent {
    data class ShowToast(val message: String) : SeatManagementEvent()
}

// ========================================================================
// TICKET MANAGEMENT
// ========================================================================
sealed class TicketManagementUIState {
    object Loading : TicketManagementUIState()
    data class Loaded(val tickets: List<TicketDetails>) : TicketManagementUIState()
    object Empty : TicketManagementUIState()
    data class Error(val message: String) : TicketManagementUIState()
}

// ========================================================================
// USER MANAGEMENT
// ========================================================================
sealed class UserManagementUIState {
    object Loading : UserManagementUIState()
    data class Loaded(val users: List<User>) : UserManagementUIState()
    object Empty : UserManagementUIState()
    object RoleChangeSuccess : UserManagementUIState()
    object BlockSuccess : UserManagementUIState()
    data class Error(val message: String) : UserManagementUIState()
}

sealed class UserManagementEvent {
    data class NavigateToDetail(val userId: Long) : UserManagementEvent()
    data class ShowToast(val message: String) : UserManagementEvent()
}

// ========================================================================
// SALES STATISTICS
// ========================================================================
sealed class SalesStatisticsUIState {
    object Loading : SalesStatisticsUIState()
    data class Loaded(
        val revenueByRoute: Map<String, Double>,
        val revenueByDateRange: Double,
        val averageTicketPrice: Double,
        val occupancyRate: Double
    ) : SalesStatisticsUIState()
    data class Error(val message: String) : SalesStatisticsUIState()
}

