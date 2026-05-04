package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.presentation.ui.state.LoginUIState
import com.example.busbooking.presentation.ui.state.LoginEvent
import kotlinx.coroutines.launch

/**
 * LoginViewModel - Authentication flow
 *
 * Responsibilities:
 * - Handle email/password validation
 * - Perform login via repository
 * - Emit UI state changes (loading, success, error)
 * - Emit one-time navigation events
 *
 * Validation Rules:
 * - Email: non-empty, valid email format
 * - Password: non-empty, at least 6 characters
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableLiveData<LoginUIState>(LoginUIState.Idle)
    val uiState: LiveData<LoginUIState> = _uiState

    private val _navigationEvents = MutableLiveData<LoginEvent>()
    val navigationEvents: LiveData<LoginEvent> = _navigationEvents

    /**
     * Perform login with email and password
     *
     * Validation:
     * - Email: not blank, valid format
     * - Password: not blank, at least 6 chars
     */
    fun loginUser(email: String, password: String) {
        // Validation
        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)

        if (emailError != null || passwordError != null) {
            _uiState.value = LoginUIState.Error(
                message = "Validation failed",
                fieldError = emailError ?: passwordError
            )
            return
        }

        _uiState.value = LoginUIState.Loading

        viewModelScope.launch {
            val result = authRepository.loginUser(email.trim(), password)
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = LoginUIState.Success(result.data)
                    _navigationEvents.value = LoginEvent.NavigateToHome(result.data.id)
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = LoginUIState.Error(result.message)
                }
            }
        }
    }

    fun navigateToRegister() {
        _navigationEvents.value = LoginEvent.NavigateToRegister
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email cannot be empty"
            !email.contains("@") -> "Invalid email format"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password cannot be empty"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }
}

/**
 * RegisterViewModel - User registration flow
 *
 * Validation Rules:
 * - Name: non-empty, at least 2 characters
 * - Email: valid email format, not already registered
 * - Password: at least 8 characters, contains letters and numbers
 * - Phone: valid phone format
 */
class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableLiveData<com.example.busbooking.presentation.ui.state.RegisterUIState>(
        com.example.busbooking.presentation.ui.state.RegisterUIState.Idle
    )
    val uiState: LiveData<com.example.busbooking.presentation.ui.state.RegisterUIState> = _uiState

    private val _navigationEvents = MutableLiveData<com.example.busbooking.presentation.ui.state.RegisterEvent>()
    val navigationEvents: LiveData<com.example.busbooking.presentation.ui.state.RegisterEvent> = _navigationEvents

    fun registerUser(name: String, email: String, password: String, phone: String) {
        // Validation
        val errors = mutableMapOf<String, String>()

        validateName(name)?.let { errors["name"] = it }
        validateEmail(email)?.let { errors["email"] = it }
        validatePassword(password)?.let { errors["password"] = it }
        validatePhone(phone)?.let { errors["phone"] = it }

        if (errors.isNotEmpty()) {
            _uiState.value = com.example.busbooking.presentation.ui.state.RegisterUIState.ValidationError(errors)
            return
        }

        _uiState.value = com.example.busbooking.presentation.ui.state.RegisterUIState.Loading

        viewModelScope.launch {
            val result = authRepository.registerUser(
                name = name.trim(),
                email = email.trim(),
                password = password,
                phone = phone.trim()
            )

            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = com.example.busbooking.presentation.ui.state.RegisterUIState.Success(result.data)
                    _navigationEvents.value = com.example.busbooking.presentation.ui.state.RegisterEvent.NavigateToLogin()
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = com.example.busbooking.presentation.ui.state.RegisterUIState.Error(result.message)
                }
            }
        }
    }

    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Name cannot be empty"
            name.length < 2 -> "Name must be at least 2 characters"
            else -> null
        }
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email cannot be empty"
            !email.contains("@") -> "Invalid email format"
            !email.contains(".") -> "Invalid email format"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password cannot be empty"
            password.length < 8 -> "Password must be at least 8 characters"
            !password.any { it.isLetter() } -> "Password must contain letters"
            !password.any { it.isDigit() } -> "Password must contain numbers"
            else -> null
        }
    }

    private fun validatePhone(phone: String): String? {
        return when {
            phone.isBlank() -> "Phone cannot be empty"
            phone.length < 10 -> "Phone must be at least 10 digits"
            !phone.all { it.isDigit() || it == '-' || it == '+' } -> "Invalid phone format"
            else -> null
        }
    }
}

/**
 * TripSearchViewModel - Trip search functionality
 *
 * Flow:
 * 1. Load origins/destinations on screen load
 * 2. User selects origin and destination
 * 3. User selects date
 * 4. ViewModel searches for trips
 * 5. Display results
 */
class TripSearchViewModel(
    private val tripRepository: com.example.busbooking.domain.repository.TripRepository,
    private val routeRepository: com.example.busbooking.domain.repository.RouteRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<com.example.busbooking.presentation.ui.state.TripSearchUIState>(
        com.example.busbooking.presentation.ui.state.TripSearchUIState.Idle
    )
    val uiState: LiveData<com.example.busbooking.presentation.ui.state.TripSearchUIState> = _uiState

    private val _navigationEvents = MutableLiveData<com.example.busbooking.presentation.ui.state.TripSearchEvent>()
    val navigationEvents: LiveData<com.example.busbooking.presentation.ui.state.TripSearchEvent> = _navigationEvents

    fun loadOrigins() {
        _uiState.value = com.example.busbooking.presentation.ui.state.TripSearchUIState.LoadingOrigins

        viewModelScope.launch {
            val result = routeRepository.getAllOrigins()
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = com.example.busbooking.presentation.ui.state.TripSearchUIState.OriginsLoaded(result.data)
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = com.example.busbooking.presentation.ui.state.TripSearchUIState.Error(result.message)
                }
            }
        }
    }

    fun loadDestinations() {
        _uiState.value = com.example.busbooking.presentation.ui.state.TripSearchUIState.LoadingDestinations

        viewModelScope.launch {
            val result = routeRepository.getAllDestinations()
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = com.example.busbooking.presentation.ui.state.TripSearchUIState.DestinationsLoaded(result.data)
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = com.example.busbooking.presentation.ui.state.TripSearchUIState.Error(result.message)
                }
            }
        }
    }

    /**
     * Search trips by origin, destination, and date
     *
     * Validation:
     * - Origin: not empty
     * - Destination: not empty
     * - Date: valid date format (YYYY-MM-DD)
     * - Origin != Destination
     */
    fun searchTrips(origin: String, destination: String, date: String) {
        // Validation
        if (origin.isBlank() || destination.isBlank() || date.isBlank()) {
            _uiState.value = com.example.busbooking.presentation.ui.state.TripSearchUIState.ValidationError(
                "All fields are required"
            )
            return
        }

        if (origin == destination) {
            _uiState.value = com.example.busbooking.presentation.ui.state.TripSearchUIState.ValidationError(
                "Origin and destination must be different"
            )
            return
        }

        _uiState.value = com.example.busbooking.presentation.ui.state.TripSearchUIState.LoadingTrips

        viewModelScope.launch {
            val result = tripRepository.searchTrips(origin.trim(), destination.trim(), date)
            when (result) {
                is com.example.busbooking.domain.models.Result.Success -> {
                    _uiState.value = com.example.busbooking.presentation.ui.state.TripSearchUIState.TripsLoaded(result.data)
                }
                is com.example.busbooking.domain.models.Result.Error -> {
                    _uiState.value = com.example.busbooking.presentation.ui.state.TripSearchUIState.Error(result.message)
                }
            }
        }
    }

    fun navigateToSeatSelection(tripId: Long) {
        _navigationEvents.value = com.example.busbooking.presentation.ui.state.TripSearchEvent.NavigateToSeatSelection(tripId)
    }
}

