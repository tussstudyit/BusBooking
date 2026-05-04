package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.domain.repository.*
import com.example.busbooking.presentation.ui.state.*
import kotlinx.coroutines.launch

/**
 * AdminDashboardViewModel - Analytics dashboard
 */
class AdminDashboardViewModel(
    private val ticketRepository: TicketRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<AdminDashboardUIState>(AdminDashboardUIState.Loading)
    val uiState: LiveData<AdminDashboardUIState> = _uiState

    fun loadAnalytics() {
        _uiState.value = AdminDashboardUIState.Loading

        viewModelScope.launch {
            try {
                val revenueResult = ticketRepository.getTotalRevenue()
                val bookingsResult = ticketRepository.getTotalBookings()
                val confirmedResult = ticketRepository.countConfirmedTickets(0, System.currentTimeMillis())
                val avgPriceResult = ticketRepository.getAverageTicketPrice()
                val refundsResult = ticketRepository.getTotalRefunds()

                val revenue = (revenueResult as? com.example.busbooking.domain.models.Result.Success)?.data ?: 0.0
                val bookings = (bookingsResult as? com.example.busbooking.domain.models.Result.Success)?.data ?: 0L
                val confirmed = (confirmedResult as? com.example.busbooking.domain.models.Result.Success)?.data ?: 0L
                val avgPrice = (avgPriceResult as? com.example.busbooking.domain.models.Result.Success)?.data ?: 0.0
                val refunds = (refundsResult as? com.example.busbooking.domain.models.Result.Success)?.data ?: 0.0

                _uiState.value = AdminDashboardUIState.Loaded(
                    totalRevenue = revenue,
                    totalBookings = bookings,
                    confirmedTickets = confirmed,
                    averagePrice = avgPrice,
                    totalRefunds = refunds
                )
            } catch (e: Exception) {
                _uiState.value = AdminDashboardUIState.Error(e.message ?: "Error loading analytics")
            }
        }
    }
}

/**
 * RouteManagementViewModel - Manage routes (CRUD)
 *
 * Validation Rules:
 * - Origin: non-empty, at least 2 characters
 * - Destination: non-empty, at least 2 characters, different from origin
 * - Distance: positive integer
 */
class RouteManagementViewModel(
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<RouteManagementUIState>(RouteManagementUIState.Loading)
    val uiState: LiveData<RouteManagementUIState> = _uiState

    private val _navigationEvents = MutableLiveData<RouteManagementEvent>()
    val navigationEvents: LiveData<RouteManagementEvent> = _navigationEvents

    fun loadRoutes() {
        _uiState.value = RouteManagementUIState.Loading

        viewModelScope.launch {
            val result = routeRepository.getAllRoutes()
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    val routes = result.data
                    _uiState.value = if (routes.isEmpty()) {
                        RouteManagementUIState.Empty
                    } else {
                        RouteManagementUIState.Loaded(routes)
                    }
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = RouteManagementUIState.Error(result.message)
                }
            }
        }
    }

    fun createRoute(origin: String, destination: String, distance: Int) {
        val errors = validateRoute(origin, destination, distance)
        if (errors.isNotEmpty()) {
            _uiState.value = RouteManagementUIState.ValidationError(errors)
            return
        }

        viewModelScope.launch {
            val result = routeRepository.createRoute(origin, destination, distance)
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = RouteManagementUIState.CreationSuccess
                    loadRoutes()
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = RouteManagementUIState.Error(result.message)
                }
            }
        }
    }

    private fun validateRoute(origin: String, destination: String, distance: Int): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (origin.isBlank()) errors["origin"] = "Origin cannot be empty"
        if (origin.length < 2) errors["origin"] = "Origin must be at least 2 characters"

        if (destination.isBlank()) errors["destination"] = "Destination cannot be empty"
        if (destination.length < 2) errors["destination"] = "Destination must be at least 2 characters"
        if (origin.equals(destination, ignoreCase = true)) errors["destination"] = "Origin and destination cannot be same"

        if (distance <= 0) errors["distance"] = "Distance must be greater than 0"

        return errors
    }
}

/**
 * BusManagementViewModel - Manage buses (CRUD)
 *
 * Validation Rules:
 * - Bus name: non-empty, unique
 * - License plate: valid format, unique
 * - Total seats: positive integer, reasonable range (20-60)
 */
class BusManagementViewModel(
    private val busRepository: BusRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<BusManagementUIState>(BusManagementUIState.Loading)
    val uiState: LiveData<BusManagementUIState> = _uiState

    private val _navigationEvents = MutableLiveData<BusManagementEvent>()
    val navigationEvents: LiveData<BusManagementEvent> = _navigationEvents

    fun loadBuses() {
        _uiState.value = BusManagementUIState.Loading

        viewModelScope.launch {
            val result = busRepository.getAllBuses()
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    val buses = result.data
                    _uiState.value = if (buses.isEmpty()) {
                        BusManagementUIState.Empty
                    } else {
                        BusManagementUIState.Loaded(buses)
                    }
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = BusManagementUIState.Error(result.message)
                }
            }
        }
    }

    fun createBus(busName: String, licensePlate: String, totalSeats: Int) {
        val errors = validateBus(busName, licensePlate, totalSeats)
        if (errors.isNotEmpty()) {
            _uiState.value = BusManagementUIState.ValidationError(errors)
            return
        }

        viewModelScope.launch {
            val result = busRepository.createBus(busName, licensePlate, totalSeats)
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = BusManagementUIState.CreationSuccess
                    loadBuses()
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = BusManagementUIState.Error(result.message)
                }
            }
        }
    }

    private fun validateBus(busName: String, licensePlate: String, totalSeats: Int): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (busName.isBlank()) errors["busName"] = "Bus name cannot be empty"
        if (licensePlate.isBlank()) errors["licensePlate"] = "License plate cannot be empty"
        if (!licensePlate.matches(Regex("[A-Z0-9-]+"))) errors["licensePlate"] = "Invalid license plate format"

        if (totalSeats < 20) errors["totalSeats"] = "Minimum 20 seats required"
        if (totalSeats > 60) errors["totalSeats"] = "Maximum 60 seats allowed"

        return errors
    }
}

/**
 * TripManagementViewModel - Manage trips (CRUD)
 *
 * Validation Rules:
 * - Route: must exist
 * - Bus: must exist
 * - Departure time: before arrival time
 * - Price: positive number
 * - Date: valid date format
 */
class TripManagementViewModel(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<TripManagementUIState>(TripManagementUIState.Loading)
    val uiState: LiveData<TripManagementUIState> = _uiState

    private val _navigationEvents = MutableLiveData<TripManagementEvent>()
    val navigationEvents: LiveData<TripManagementEvent> = _navigationEvents

    fun loadTrips() {
        _uiState.value = TripManagementUIState.Loading

        viewModelScope.launch {
            tripRepository.getAllTrips().collect { trips ->
                _uiState.value = if (trips.isEmpty()) {
                    TripManagementUIState.Empty
                } else {
                    TripManagementUIState.Loaded(trips)
                }
            }
        }
    }

    fun createTrip(
        routeId: Long,
        busId: Long,
        departureTime: Long,
        arrivalTime: Long,
        price: Double,
        tripDate: String
    ) {
        val errors = validateTrip(departureTime, arrivalTime, price, tripDate)
        if (errors.isNotEmpty()) {
            _uiState.value = TripManagementUIState.ValidationError(errors)
            return
        }

        viewModelScope.launch {
            val result = tripRepository.createTrip(
                routeId, busId, departureTime, arrivalTime, price, tripDate
            )
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = TripManagementUIState.CreationSuccess
                    loadTrips()
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = TripManagementUIState.Error(result.message)
                }
            }
        }
    }

    private fun validateTrip(
        departureTime: Long,
        arrivalTime: Long,
        price: Double,
        tripDate: String
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (departureTime >= arrivalTime) errors["times"] = "Departure must be before arrival"
        if (price <= 0) errors["price"] = "Price must be greater than 0"
        if (tripDate.isBlank()) errors["date"] = "Date cannot be empty"

        return errors
    }
}

/**
 * UserManagementViewModel - Manage users (CRUD + role change)
 */
class UserManagementViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<UserManagementUIState>(UserManagementUIState.Loading)
    val uiState: LiveData<UserManagementUIState> = _uiState

    private val _navigationEvents = MutableLiveData<UserManagementEvent>()
    val navigationEvents: LiveData<UserManagementEvent> = _navigationEvents

    fun loadUsers() {
        _uiState.value = UserManagementUIState.Loading

        viewModelScope.launch {
            val result = userRepository.getAllUsers(50, 0)
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    val users = result.data
                    _uiState.value = if (users.isEmpty()) {
                        UserManagementUIState.Empty
                    } else {
                        UserManagementUIState.Loaded(users)
                    }
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = UserManagementUIState.Error(result.message)
                }
            }
        }
    }

    fun changeUserRole(userId: Long, newRole: String) {
        viewModelScope.launch {
            val result = userRepository.changeUserRole(userId, newRole)
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = UserManagementUIState.RoleChangeSuccess
                    loadUsers()
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = UserManagementUIState.Error(result.message)
                }
            }
        }
    }

    fun blockUser(userId: Long) {
        viewModelScope.launch {
            val result = userRepository.blockUser(userId)
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = UserManagementUIState.BlockSuccess
                    loadUsers()
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = UserManagementUIState.Error(result.message)
                }
            }
        }
    }
}

/**
 * SalesStatisticsViewModel - View historical sales data and analytics
 */
class SalesStatisticsViewModel(
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<SalesStatisticsUIState>(SalesStatisticsUIState.Loading)
    val uiState: LiveData<SalesStatisticsUIState> = _uiState

    fun loadStatistics(startTime: Long, endTime: Long) {
        _uiState.value = SalesStatisticsUIState.Loading

        viewModelScope.launch {
            try {
                val revenueResult = ticketRepository.getRevenueByDateRange(startTime, endTime)
                val avgPriceResult = ticketRepository.getAverageTicketPrice()

                val revenue = (revenueResult as? com.example.busbooking.domain.models.Result.Success)?.data ?: 0.0
                val avgPrice = (avgPriceResult as? com.example.busbooking.domain.models.Result.Success)?.data ?: 0.0

                _uiState.value = SalesStatisticsUIState.Loaded(
                    revenueByRoute = emptyMap(),  // TODO: Group by route
                    revenueByDateRange = revenue,
                    averageTicketPrice = avgPrice,
                    occupancyRate = 0.0  // TODO: Calculate occupancy
                )
            } catch (e: Exception) {
                _uiState.value = SalesStatisticsUIState.Error(e.message ?: "Error loading statistics")
            }
        }
    }
}

